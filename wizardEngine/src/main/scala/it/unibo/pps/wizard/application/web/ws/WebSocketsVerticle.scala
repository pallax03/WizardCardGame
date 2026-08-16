package it.unibo.pps.wizard.application.web.ws

import io.vertx.core.AbstractVerticle
import io.vertx.ext.web.Router
import it.unibo.pps.wizard.application.web.*
import it.unibo.pps.wizard.engine.lobby.Lobby
import it.unibo.pps.wizard.engine.ports.LobbyStatePort
import it.unibo.pps.wizard.engine.ports.WebSocketsPort

import scala.util.Success
import it.unibo.pps.wizard.util.FutureSyntax.*

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
    router.route("/lobby/:lobbyId/player/:playerId").handler: ctx =>
      val req = ctx.request()
      (req.extractLobbyId, req.extractPlayerId) match
        case (Some(lobbyId), Some(playerId)) =>
          req.toWebSocket.onComplete: res =>
            if res.succeeded() then
              val ws = res.result()
              lobbyStatePort.getLobby(lobbyId).onVertxComplete(ctx):
                case Success(Some(lobby: Lobby)) if lobby.players.exists(_.id == playerId) =>
                  wsPortAdapter.subscribeToLobbyEvents(lobbyId, playerId, ws)
                case _ => ws.close(403, "Forbidden")
        case _ =>
          req.response().setStatusCode(400).end("Missing lobbyId")

    vertx.createHttpServer().requestHandler(router).listen(port)
