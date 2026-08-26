package it.unibo.pps.wizard.engine.adapters.redis

import io.vertx.redis.client.{Command, Redis, Request}
import it.unibo.pps.wizard.codecs.engine.model.core.state.GameStateCodecs.given
import it.unibo.pps.wizard.codecs.syntax.CodecSyntax.*
import it.unibo.pps.wizard.engine.configuration.*
import it.unibo.pps.wizard.engine.lobby.LobbyId
import it.unibo.pps.wizard.engine.model.basic.*
import it.unibo.pps.wizard.engine.model.core.*
import it.unibo.pps.wizard.engine.model.core.InconsistentState
import it.unibo.pps.wizard.engine.model.core.state.GameState
import it.unibo.pps.wizard.engine.model.core.state.PlayerGameState
import it.unibo.pps.wizard.engine.model.core.state.ServerGameState
import it.unibo.pps.wizard.engine.model.events.*
import it.unibo.pps.wizard.engine.ports.InboundPort
import it.unibo.pps.wizard.engine.ports.OutboundPort
import it.unibo.pps.wizard.util.ChannelsKeys
import it.unibo.pps.wizard.util.FutureSyntax.*

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class RedisInboundAdapter(
    private val redisClient: Redis,
    private val outboundPort: OutboundPort
) extends InboundPort:

  private def decodeGameState(rawGameState: String): ServerGameState = rawGameState.decodeAs[ServerGameState] match
    case Right(state) => state
    case Left(err) => throw GameException(InconsistentState.CorruptedState(err.toString))

  /** @inheritdoc */
  override def getState(lobbyId: LobbyId, playerId: PlayerId): Future[PlayerGameState] =
    val key = ChannelsKeys.game(lobbyId)
    redisClient
      .send(Request.cmd(Command.GET).arg(key))
      .asScala
      .map:
        case null => throw new RuntimeException("Game not found")
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
              outboundPort.publish(lobbyId, initialState.events *)

  /** @inheritdoc */
  override def submitAction(lobbyId: LobbyId, action: GameAction): Future[Unit] =
    val key = ChannelsKeys.game(lobbyId)
    redisClient
      .send(Request.cmd(Command.GET).arg(key))
      .asScala
      .flatMap:
        case null => Future.unit
        case response =>
          GameEngine.processAction(decodeGameState(response.toString), action) match
            case Left(error) =>
              outboundPort.publish(lobbyId, FailureEvent.ActionFailed(action.playerId, error))
              Future.unit
            case Right(newState) =>
              val saveReq = newState.state match
                case _: GameState.Ended => Request.cmd(Command.DEL).arg(key)
                case _ => Request.cmd(Command.SET).arg(key).arg(newState.state.toJson)

              redisClient
                .send(saveReq)
                .asScala
                .map: _ =>
                  outboundPort.publish(lobbyId, newState.events*)
