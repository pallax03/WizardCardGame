package it.unibo.pps.wizard.engine.adapters.redis

import io.vertx.redis.client.{Command, Redis, Request}
import it.unibo.pps.wizard.engine.configuration.*
import it.unibo.pps.wizard.engine.lobby.LobbyId
import it.unibo.pps.wizard.engine.model.basic.*
import it.unibo.pps.wizard.engine.model.core.*
import it.unibo.pps.wizard.engine.model.events.*
import it.unibo.pps.wizard.engine.ports.{InboundPort, OutboundPort}
import it.unibo.pps.wizard.codecs.syntax.CodecSyntax.*
import it.unibo.pps.wizard.codecs.engine.model.core.state.GameStateCodecs.given
import it.unibo.pps.wizard.engine.model.core.state.{GameState, ServerGameState, PlayerGameState}
import it.unibo.pps.wizard.util.ChannelsKeys

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import it.unibo.pps.wizard.util.FutureSyntax.*

class RedisInboundAdapter(
    private val redisClient: Redis,
    private val outboundPort: OutboundPort
) extends InboundPort:

  // todo: related issue https://github.com/pallax03/WizardCardGame/issues/39
  override def getState(lobbyId: LobbyId, playerId: PlayerId): Future[PlayerGameState] =
    val key = ChannelsKeys.game(lobbyId)
    redisClient.send(Request.cmd(Command.GET).arg(key)).asScala.map:
      case null => throw new RuntimeException("Game not found")
      case response => 
        val serverState = response.toString.decodeAs[ServerGameState] match
          case Right(state) => state
          case Left(err) => throw GameException(GameError.InconsistentState(InconsistentStateReasons.CorruptedState(err.toString)))
        PlayerGameState.from(serverState, playerId) match
          case Right(state) => state
          case Left(err) => throw GameException(err)

  override def startGame(lobbyId: LobbyId, players: List[PlayerId], config: GameConfiguration): Future[Unit] =
    val key = ChannelsKeys.game(lobbyId)
    val req = Request.cmd(Command.GET).arg(key)
    redisClient.send(req).asScala.flatMap:
      case null => 
        val playersIds = config.players.map(_.id)
        GameEngine.initializeGame(playersIds) match
          case Right(initialState) =>
            val setReq = Request.cmd(Command.SET).arg(key).arg(initialState.state.toJson)
            redisClient.send(setReq).asScala.map: _ =>
              outboundPort.publish(lobbyId, LifecycleEvent.GameStarted(playersIds))
              outboundPort.publish(lobbyId, initialState.events*)
          case Left(_) => Future.successful(())

  override def submitAction(lobbyId: LobbyId, action: GameAction): Future[Unit] =
    val key = ChannelsKeys.game(lobbyId)
    redisClient.send(Request.cmd(Command.GET).arg(key)).asScala.flatMap:
      case null => Future.successful(())
      case response =>
        val oldState = response.toString.decodeAs[ServerGameState] match
          case Right(state) => state
          case Left(err) => throw GameException(GameError.InconsistentState(InconsistentStateReasons.CorruptedState(err.toString)))
        GameEngine.processAction(oldState, action) match
          case Left(error) =>
            outboundPort.publish(lobbyId, FailureEvent.ActionFailed(action.playerId, error))
            Future.successful(())
          case Right(newState) =>
            val saveReq = newState.state match
              case _: GameState.Ended => Request.cmd(Command.DEL).arg(key)
              case _ => Request.cmd(Command.SET).arg(key).arg(newState.state.toJson)
            
            redisClient.send(saveReq).asScala.map: _ =>
              outboundPort.publish(lobbyId, newState.events*)