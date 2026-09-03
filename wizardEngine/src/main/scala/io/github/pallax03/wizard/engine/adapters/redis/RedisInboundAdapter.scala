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
import io.github.pallax03.wizard.engine.ports.{InboundPort, OutboundPort}
import io.github.pallax03.wizard.util.ChannelsKeys
import io.github.pallax03.wizard.util.FutureSyntax.*

class RedisInboundAdapter(
    private val redisClient: Redis,
    private val outboundPort: OutboundPort,
    private val recoveryPort: io.github.pallax03.wizard.engine.ports.GameRecoveryPort
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
        val isNewRound = newState.events.exists(_.isInstanceOf[ProgressEvent.RoundStarted])
        if isNewRound then
          val cpSave = Request.cmd(Command.SET).arg(checkpointKey).arg(newState.state.toJson)
          List(mainSave, cpSave)
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
        val playersIds = config.players.map(_.id)
        val initialState = GameEngine.initializeGame(playersIds)
        redisClient
          .send(
            Request.cmd(Command.SET).arg(ChannelsKeys.game(lobbyId)).arg(initialState.state.toJson)
          )
          .asScala
          .map: _ =>
            outboundPort.publish(lobbyId, LifecycleEvent.GameStarted(playersIds))
            outboundPort.publish(lobbyId, initialState.events*)

  /** @inheritdoc */
  override def submitAction(lobbyId: LobbyId, action: GameAction): Future[Either[GameError, Unit]] =
    withRecovery(lobbyId):
      fetchGameState(lobbyId).flatMap:
        case None => Future.successful(Right(()))
        case Some(state) =>
          GameEngine.processAction(state, action) match
            case Left(error) => Future.successful(Left(error))
            case Right(newState) =>
              saveState(lobbyId, newState).map: _ =>
                outboundPort.publish(lobbyId, newState.events*)
                Right(())
