package io.github.pallax03.wizard.engine.adapters.redis

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

import io.vertx.redis.client.{Command, Redis, Request}

import io.github.pallax03.wizard.codecs.engine.model.core.state.GameStateCodecs.given
import io.github.pallax03.wizard.codecs.syntax.CodecSyntax._
import io.github.pallax03.wizard.engine.configuration._
import io.github.pallax03.wizard.engine.lobby.LobbyId
import io.github.pallax03.wizard.engine.model.basic._
import io.github.pallax03.wizard.engine.model.core.InconsistentState._
import io.github.pallax03.wizard.engine.model.core._
import io.github.pallax03.wizard.engine.model.core.state._
import io.github.pallax03.wizard.engine.model.events._
import io.github.pallax03.wizard.engine.ports.{InboundPort, OutboundPort}
import io.github.pallax03.wizard.util.ChannelsKeys
import io.github.pallax03.wizard.util.FutureSyntax._

class RedisInboundAdapter(
    private val redisClient: Redis,
    private val outboundPort: OutboundPort
) extends InboundPort:

  private def decodeGameState(rawGameState: String): ServerGameState =
    rawGameState.decodeAs[ServerGameState] match
      case Right(state) => state
      case Left(err)    => throw GameException(CorruptedState(err.toString))

  /** @inheritdoc */
  override def getState(lobbyId: LobbyId, playerId: PlayerId): Future[PlayerGameState] =
    val key = ChannelsKeys.game(lobbyId)
    redisClient
      .send(Request.cmd(Command.GET).arg(key))
      .asScala
      .map:
        case null     => throw GameException(GameNotFound)
        case response => PlayerGameState.from(decodeGameState(response.toString), playerId)

  /** @inheritdoc */
  override def startGame(
      lobbyId: LobbyId,
      players: List[PlayerId],
      config: GameConfiguration
  ): Future[Unit] =
    val key = ChannelsKeys.game(lobbyId)
    val req = Request.cmd(Command.GET).arg(key)
    redisClient
      .send(req)
      .asScala
      .flatMap:
        case null =>
          val playersIds = config.players.map(_.id)
          val initialState = GameEngine.initializeGame(playersIds)
          val setReq = Request.cmd(Command.SET).arg(key).arg(initialState.state.toJson)
          redisClient
            .send(setReq)
            .asScala
            .map: _ =>
              outboundPort.publish(lobbyId, LifecycleEvent.GameStarted(playersIds))
              outboundPort.publish(lobbyId, initialState.events*)

  /** @inheritdoc */
  override def submitAction(lobbyId: LobbyId, action: GameAction): Future[Either[GameError, Unit]] =
    val key = ChannelsKeys.game(lobbyId)
    redisClient
      .send(Request.cmd(Command.GET).arg(key))
      .asScala
      .flatMap:
        case null => Future.successful(Right(()))
        case response =>
          GameEngine.processAction(decodeGameState(response.toString), action) match
            case Left(error) =>
              Future.successful(Left(error))
            case Right(newState) =>
              val saveReqs = newState.state match
                case _: GameState.Ended =>
                  List(
                    Request.cmd(Command.DEL).arg(key),
                    Request.cmd(Command.DEL).arg(ChannelsKeys.gameCheckpoint(lobbyId))
                  )
                case _ =>
                  val mainSave = Request.cmd(Command.SET).arg(key).arg(newState.state.toJson)
                  val isNewRound = newState.events.exists {
                    case _: ProgressEvent.RoundStarted => true
                    case _                             => false
                  }
                  if isNewRound then
                    List(
                      mainSave,
                      Request
                        .cmd(Command.SET)
                        .arg(ChannelsKeys.gameCheckpoint(lobbyId))
                        .arg(newState.state.toJson)
                    )
                  else List(mainSave)

              Future
                .sequence(saveReqs.map(r => redisClient.send(r).asScala))
                .map: _ =>
                  outboundPort.publish(lobbyId, newState.events*)
                  Right(())
