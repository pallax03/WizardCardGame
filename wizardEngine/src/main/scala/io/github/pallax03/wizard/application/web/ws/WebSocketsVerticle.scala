package io.github.pallax03.wizard.application.web.ws

import scala.util.Success

import io.vertx.core.AbstractVerticle
import io.vertx.core.http.HttpServerOptions
import io.vertx.ext.web.Router

import io.github.pallax03.wizard.codecs.engine.lobby.LobbyCodecs.given
import io.github.pallax03.wizard.codecs.syntax.CodecSyntax.toJson
import io.github.pallax03.wizard.engine.lobby.{Lobby, LobbyError, LobbyId}
import io.github.pallax03.wizard.engine.ports.{LobbyStatePort, WebSocketsPort}
import io.github.pallax03.wizard.util.FutureSyntax.*

class WebSocketsVerticle(
    wsPortAdapter: WebSocketsPort,
    lobbyStatePort: LobbyStatePort,
    port: Int
) extends AbstractVerticle:

  private val WS_IDLE_TIMEOUT_SECONDS: Int = 60

  override def start(): Unit =
    val router = Router.router(vertx)
    router
      .route("/lobby/:lobbyId")
      .handler: ctx =>
        val req = ctx.request()
        val lobbyIdStr = req.getParam("lobbyId")
        val secret = req.getParam("secret")
        if lobbyIdStr == null || secret == null then {
          // todo: development / production
          req
            .response()
            .setStatusCode(400)
            .end("try with <url>/lobby/<lobbyId>?secret=<player_secret>")
        } else
          req.toWebSocket.onComplete: res =>
            if res.succeeded() then
              val ws = res.result()
              lobbyStatePort
                .getLobby(LobbyId(lobbyIdStr))
                .onVertxComplete(ctx):
                  case Success(Some(lobby: Lobby)) =>
                    lobby.authenticate(secret) match
                      case Right(player) =>
                        wsPortAdapter.subscribeToLobbyEvents(LobbyId(lobbyIdStr), player.id, ws)
                      case Left(err) =>
                        ws.close(401, err.toJson)
                  case _ =>
                    ws.close(404, LobbyError.LobbyNotFound.toJson)
    val options = HttpServerOptions().setIdleTimeout(WS_IDLE_TIMEOUT_SECONDS)
    vertx.createHttpServer(options).requestHandler(router).listen(port)
