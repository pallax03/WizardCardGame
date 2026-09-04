package io.github.pallax03.wizard.application.web.ws

import scala.util.Success

import io.vertx.core.AbstractVerticle
import io.vertx.core.http.HttpServerOptions
import io.vertx.ext.web.Router

import io.github.pallax03.wizard.engine.lobby.{Lobby, LobbyId}
import io.github.pallax03.wizard.engine.ports.{LobbyStatePort, WebSocketsPort}
import io.github.pallax03.wizard.util.FutureSyntax.*

class WebSocketsVerticle(
    wsPortAdapter: WebSocketsPort,
    lobbyStatePort: LobbyStatePort,
    port: Int
) extends AbstractVerticle:

  override def start(): Unit =
    val router = Router.router(vertx)
    router
      .route("/ws/lobby/:lobbyId")
      .handler: ctx =>
        val req = ctx.request()
        val lobbyIdStr = req.getParam("lobbyId")
        val secret = req.getParam("secret")

        if lobbyIdStr == null || secret == null then
          req.response().setStatusCode(400).end("Missing lobbyId or secret")
        else
          req.toWebSocket.onComplete: res =>
            if res.succeeded() then
              val ws = res.result()
              lobbyStatePort
                .getLobby(LobbyId(lobbyIdStr))
                .onVertxComplete(ctx):
                  case Success(Some(lobby: Lobby)) =>
                    lobby.players.find(_.secret.contains(secret)) match
                      case Some(player) =>
                        wsPortAdapter.subscribeToLobbyEvents(LobbyId(lobbyIdStr), player.id, ws)
                      case None => ws.close(403, "Forbidden")
                  case _ => ws.close(404, "Not Found")

    val options = HttpServerOptions().setIdleTimeout(60)
    vertx.createHttpServer(options).requestHandler(router).listen(port)
