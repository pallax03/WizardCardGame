package it.unibo.pps.wizard.application.web.ws

import io.vertx.core.AbstractVerticle
import io.vertx.ext.web.Router
import it.unibo.pps.wizard.application.web._
import it.unibo.pps.wizard.engine.ports.{LobbyStatePort, WebSocketsPort}

import scala.util.Success
import scala.concurrent.ExecutionContext.Implicits.global

/**
 * Verticle responsible for managing WebSocket connections.
 * 
 * @param wsPortAdapter the adapter responsible for handling WebSocket connections.
 * @param lobbyStatePort the port responsible for managing lobby states.
 * @param port the port on which the WebSocket server will be exposed.
 */
class WebSocketsVerticle(
                          wsPortAdapter: WebSocketsPort,
                          lobbyStatePort: LobbyStatePort,
                          port: Int
                        ) extends AbstractVerticle:

  override def start(): Unit =
    val router = Router.router(vertx)
    router.route("/socket.io/lobby/:lobbyId/player/:playerId").handler { ctx =>
      val req = ctx.request()
      val lobbyIdOpt = req.extractLobbyId
      val playerIdOpt = req.extractPlayerId
      println(s"request received from /lobby/$lobbyIdOpt/player/$playerIdOpt")
      (lobbyIdOpt, playerIdOpt) match
        case (Some(lobbyId), Some(playerId)) =>
          lobbyStatePort.getLobby(lobbyId.toString).onComplete:
            case Success(Some(lobby)) if lobby.players.exists(_.id == playerId) =>
              req.toWebSocket.onComplete: res =>
                if res.succeeded() then
                  wsPortAdapter.subscribeToLobbyEvents(lobbyId, playerId, res.result())
                else
                  ctx.fail(res.cause())
            case _ =>
              req.response().setStatusCode(403).end("Forbidden")
        case _ =>
          req.response().setStatusCode(400).end("Missing lobbyId or playerId")
    }

    vertx.createHttpServer().requestHandler(router).listen(port)
