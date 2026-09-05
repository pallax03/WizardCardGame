package io.github.pallax03.wizard.engine.adapters.redis

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import cats.syntax.all.*
import io.vertx.redis.client.{Command, Redis, Request}
import io.github.pallax03.wizard.codecs.engine.model.core.state.GameStateCodecs.given
import io.github.pallax03.wizard.codecs.syntax.CodecSyntax.*
import io.github.pallax03.wizard.engine.configuration.*
import io.github.pallax03.wizard.engine.lobby.LobbyId
import io.github.pallax03.wizard.engine.model.basic.*
import io.github.pallax03.wizard.engine.model.core.*
import io.github.pallax03.wizard.engine.model.core.InconsistentState.*
import io.github.pallax03.wizard.engine.model.core.state.*
import io.github.pallax03.wizard.engine.model.events.*
import io.github.pallax03.wizard.engine.model.rules.FallbackStrategy
import io.github.pallax03.wizard.engine.ports.{GameRecoveryPort, InboundPort, LobbyStatePort, OutboundPort}
import io.github.pallax03.wizard.util.ChannelsKeys
import io.github.pallax03.wizard.util.FutureSyntax.*

class RedisInboundAdapter(
    private val redisClient: Redis,
    private val outboundPort: OutboundPort,
    private val recoveryPort: GameRecoveryPort,
    private val lobbyStatePort: LobbyStatePort
) extends InboundPort:

  private def decodeGameState(rawGameState: String): ServerGameState =
    rawGameState.decodeAs[ServerGameState] match
      case Right(state) => state
      case Left(err)    => throw GameException(CorruptedState(err.toString))

  private def fetchGameState(lobbyId: LobbyId): Future[Option[ServerGameState]] =
    redisClient
      .send(Request.cmd(Command.GET).arg(ChannelsKeys.game(lobbyId)))
      .asScala
      .map(Option(_).map(r => decodeGameState(r.toString)))

  private def withRecovery[T](lobbyId: LobbyId)(action: => Future[T]): Future[T] =
    action.recoverWith:
      case ge: GameException =>
        recoveryPort
          .attemptRecovery(lobbyId, ge)
          .flatMap: recovered =>
            Future.failed(
              if recovered then RecoveredGameException(ge) else AbortedGameException(ge)
            )

  private def saveState(lobbyId: LobbyId, newState: GameEngine): Future[Unit] =
    val key = ChannelsKeys.game(lobbyId)
    val checkpointKey = ChannelsKeys.gameCheckpoint(lobbyId)

    val reqs = newState.state match
      case _: GameState.Ended =>
        List(Request.cmd(Command.DEL).arg(key), Request.cmd(Command.DEL).arg(checkpointKey))
      case _ =>
        val mainSave = Request.cmd(Command.SET).arg(key).arg(newState.state.toJson)
        if newState.events.exists(_.isInstanceOf[ProgressEvent.RoundScored]) then
          List(mainSave, Request.cmd(Command.SET).arg(checkpointKey).arg(newState.state.toJson))
        else List(mainSave)

    Future.sequence(reqs.map(r => redisClient.send(r).asScala)).void

  /** @inheritdoc */
  override def getState(lobbyId: LobbyId, playerId: PlayerId): Future[PlayerGameState] =
    withRecovery(lobbyId):
      fetchGameState(lobbyId).map:
        case Some(state) => PlayerGameState.from(state, playerId)
        case None        => throw GameException(GameNotFound)

  /** @inheritdoc */
  override def startGame(
      lobbyId: LobbyId,
      players: List[PlayerId],
      config: GameConfiguration
  ): Future[Unit] =
    fetchGameState(lobbyId).flatMap:
      case Some(_) => Future.unit
      case None =>
        val initialState = GameEngine.initializeGame(players)
        redisClient
          .send(
            Request.cmd(Command.SET).arg(ChannelsKeys.game(lobbyId)).arg(initialState.state.toJson)
          )
          .asScala
          .map: _ =>
            outboundPort.publish(lobbyId, initialState.events*)

  /** @inheritdoc */
  override def resumeGame(lobbyId: LobbyId): Future[Unit] =
    fetchGameState(lobbyId).flatMap:
      case Some(state) =>
        outboundPort.publish(lobbyId, LifecycleEvent.GameResumed(state.playersIds))
        Future.unit
      case None =>
        Future.failed(GameException(GameNotFound))

  /** @inheritdoc */
  override def submitAction(lobbyId: LobbyId, action: GameAction): Future[Either[GameError, Unit]] =
    withRecovery(lobbyId):
      fetchGameState(lobbyId).flatMap:
        case None => Future.successful(Right(()))
        case Some(state) =>
          GameEngine.processAction(state, action) match
            case Left(error) => Future.successful(Left(error))
            case Right(newState) =>
              val playerId = action.playerId
              val clearTimer = redisClient.send(Request.cmd(Command.DEL).arg(ChannelsKeys.turnTimer(lobbyId, playerId))).asScala
              val clearStrikes = redisClient.send(Request.cmd(Command.DEL).arg(ChannelsKeys.afkStrikes(lobbyId, playerId))).asScala
              
              saveState(lobbyId, newState)
                .zip(clearTimer.zip(clearStrikes))
                .map: _ =>
                  outboundPort.publish(lobbyId, newState.events*)
                  Right(())

  /** @inheritdoc */
  override def handleTimeout(lobbyId: LobbyId, playerId: PlayerId): Future[Unit] =
    val clearTimerF = redisClient.send(Request.cmd(Command.DEL).arg(ChannelsKeys.turnTimer(lobbyId, playerId))).asScala
    clearTimerF.flatMap:
      case resp if resp != null && resp.toInteger == 1 =>
        getState(lobbyId, playerId).flatMap: playerGameState =>
            playerGameState.pendingInvitation(playerId) match
              case Some(invitationEvent: InvitationEvent) => 
                val fallbackAction = FallbackStrategy.fallbackMove(invitationEvent)
                val strikesKey = ChannelsKeys.afkStrikes(lobbyId, playerId)
                lobbyStatePort.getLobby(lobbyId).flatMap:
                  case None => Future.unit
                  case Some(lobby) =>
                    redisClient.send(Request.cmd(Command.INCR).arg(strikesKey)).asScala.flatMap: strikes =>
                      if strikes.toLong >= lobby.configuration.maxStrikes then
                        lobbyStatePort.setPlayerOnlineStatus(lobbyId, playerId, false).flatMap: _ =>
                          lobbyStatePort.saveLobby(lobby.copy(status = io.github.pallax03.wizard.engine.lobby.LobbyStatus.PAUSED))
                      else Future.unit
                    .flatMap: _ =>
                      outboundPort.publish(lobbyId, io.github.pallax03.wizard.engine.model.events.SystemEvent.timeout(playerId))
                      submitAction(lobbyId, fallbackAction).void
              case None => Future.unit // maybe a GameException... #82 issue: https://github.com/pallax03/WizardCardGame/issues/82
      case _ => Future.unit
