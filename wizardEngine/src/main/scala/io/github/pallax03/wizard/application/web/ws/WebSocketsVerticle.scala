package io.github.pallax03.wizard.application.web.ws

import io.vertx.core.AbstractVerticle
import io.vertx.core.http.HttpServerOptions
import io.vertx.ext.web.Router
import io.github.pallax03.wizard.application.web._
import io.github.pallax03.wizard.engine.lobby.Lobby
import io.github.pallax03.wizard.engine.ports.LobbyStatePort
import io.github.pallax03.wizard.engine.ports.WebSocketsPort
import io.github.pallax03.wizard.util.FutureSyntax._

import scala.util.Success

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
    router
      .route("/lobby/:lobbyId/player/:playerId")
      .handler: ctx =>
        val req = ctx.request()
        (req.extractLobbyId, req.extractPlayerId) match
          case (Some(lobbyId), Some(playerId)) =>
            req.toWebSocket.onComplete: res =>
              if res.succeeded() then
                val ws = res.result()
                lobbyStatePort
                  .getLobby(lobbyId)
                  .onVertxComplete(ctx):
                    case Success(Some(lobby: Lobby)) if lobby.players.exists(_.id == playerId) =>
                      wsPortAdapter.subscribeToLobbyEvents(lobbyId, playerId, ws)
                    case _ => ws.close(403, "Forbidden")
          case _ =>
            req.response().setStatusCode(400).end("Missing lobbyId")

    val options = HttpServerOptions().setIdleTimeout(60)
    vertx.createHttpServer(options).requestHandler(router).listen(port)
