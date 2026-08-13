package it.unibo.pps.wizard.engine.adapters.redis

import io.vertx.redis.client.{Command, Redis, Request}
import it.unibo.pps.wizard.engine.configuration.*
import it.unibo.pps.wizard.engine.lobby.LobbyId
import it.unibo.pps.wizard.engine.model.basic.*
import it.unibo.pps.wizard.engine.model.core.*
import it.unibo.pps.wizard.engine.model.events.*
import it.unibo.pps.wizard.engine.ports.{InboundPort, OutboundPort}
import it.unibo.pps.wizard.codecs.syntax.CodecSyntax.*
import it.unibo.pps.wizard.codecs.engine.model.core.GameStateCodecs.given
import it.unibo.pps.wizard.util.ChannelsKeys

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import it.unibo.pps.wizard.util.FutureSyntax.*

class RedisInboundAdapter(
    private val redisClient: Redis,
    private val outboundPort: OutboundPort
) extends InboundPort:


  override def getState(lobbyId: LobbyId, playerId: PlayerId): Future[GameState] =
    val req = Request.cmd(Command.GET).arg(ChannelsKeys.game(lobbyId))
    redisClient.send(req).asScala.map:
      case null => throw new IllegalStateException("Game not running")
      case response => response.toString.decodeAs[GameState].toOption.get

  override def startGame(lobbyId: LobbyId, players: List[PlayerId], config: GameConfiguration): Future[Unit] =
    val key = ChannelsKeys.game(lobbyId)
    val req = Request.cmd(Command.GET).arg(key)
    redisClient.send(req).asScala.flatMap:
      case null => 
        val playersIds = config.players.map(_.id)
        val initialState = GameEngine.initializeGame(playersIds)
        val setReq = Request.cmd(Command.SET).arg(key).arg(initialState.state.toJson)
        redisClient.send(setReq).asScala.map: _ =>
          outboundPort.publish(lobbyId, LifecycleEvent.GameStarted(playersIds))
          outboundPort.publish(lobbyId, initialState.events*)
      case _ => Future.successful(())

  override def submitAction(lobbyId: LobbyId, action: GameAction): Future[Unit] =
    val key = ChannelsKeys.game(lobbyId)
    
    def attempt(): Future[Unit] =
      redisClient.send(Request.cmd(Command.WATCH).arg(key)).asScala.flatMap: _ =>
        redisClient.send(Request.cmd(Command.GET).arg(key)).asScala.flatMap: 
          case null => 
            redisClient.send(Request.cmd(Command.UNWATCH)).asScala.map(_ => ())
          case response =>
            val oldState = response.toString.decodeAs[GameState].toOption.get
            GameEngine.processAction(oldState, action) match
              case Left(error) =>
                redisClient.send(Request.cmd(Command.UNWATCH)).asScala.map: _ =>
                  outboundPort.publish(lobbyId, FailureEvent.ActionFailed(action.playerId, error))
              case Right(newState) =>
                redisClient.send(Request.cmd(Command.MULTI))
                redisClient.send(Request.cmd(Command.SET).arg(key).arg(newState.state.toJson))
                redisClient.send(Request.cmd(Command.EXEC)).asScala.flatMap:
                  case null => attempt()
                  case _ => 
                    newState.state match
                      case _: GameState.Ended => 
                        redisClient.send(Request.cmd(Command.DEL).arg(key))
                        outboundPort.publish(lobbyId, newState.events*)
                        Future.successful(())
                      case _ =>
                        outboundPort.publish(lobbyId, newState.events*)
                        Future.successful(())
    attempt()