package io.github.pallax03.wizard.engine.adapters.redis

import io.vertx.redis.client.Command
import io.vertx.redis.client.Redis
import io.vertx.redis.client.Request
import io.github.pallax03.wizard.codecs.engine.model.core.state.GameStateCodecs.given
import io.github.pallax03.wizard.codecs.syntax.CodecSyntax._
import io.github.pallax03.wizard.engine.configuration._
import io.github.pallax03.wizard.engine.lobby.LobbyId
import io.github.pallax03.wizard.engine.model.basic._
import io.github.pallax03.wizard.engine.model.core.InconsistentState._
import io.github.pallax03.wizard.engine.model.core._
import io.github.pallax03.wizard.engine.model.core.state._
import io.github.pallax03.wizard.engine.model.events._
import io.github.pallax03.wizard.engine.ports.InboundPort
import io.github.pallax03.wizard.engine.ports.OutboundPort
import io.github.pallax03.wizard.util.ChannelsKeys
import io.github.pallax03.wizard.util.FutureSyntax._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

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
              val saveReq = newState.state match
                case _: GameState.Ended => Request.cmd(Command.DEL).arg(key)
                case _ => Request.cmd(Command.SET).arg(key).arg(newState.state.toJson)

              redisClient
                .send(saveReq)
                .asScala
                .map: _ =>
                  outboundPort.publish(lobbyId, newState.events*)
                  Right(())
