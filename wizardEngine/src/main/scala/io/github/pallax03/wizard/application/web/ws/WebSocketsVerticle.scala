package io.github.pallax03.wizard.application.web.ws

import io.github.pallax03.wizard.application.web.ResponseErrors

import scala.util.Success
import io.vertx.core.AbstractVerticle
import io.vertx.core.http.HttpServerOptions
import io.vertx.ext.web.Router
import io.github.pallax03.wizard.engine.lobby.{Lobby, LobbyId}
import io.github.pallax03.wizard.engine.ports.{LobbyStatePort, WebSocketsPort}
import io.github.pallax03.wizard.util.FutureSyntax.*

import io.github.pallax03.wizard.codecs.syntax.CodecSyntax.toJson
import io.github.pallax03.wizard.codecs.http.ResponseErrorsCodec.given 

class WebSocketsVerticle(
    wsPortAdapter: WebSocketsPort,
    lobbyStatePort: LobbyStatePort,
    port: Int
) extends AbstractVerticle:
  
  override def start(): Unit =
    val router = Router.router(vertx)
    router
      .route("/lobby/:lobbyId")
      .handler: ctx =>
        val req = ctx.request()
        val lobbyIdStr = req.getParam("lobbyId")
        val secret = req.getParam("secret")
        if lobbyIdStr == null || secret == null then
          val err: ResponseErrors = ResponseErrors.CustomBadRequest("Invalid Request: try with <url>/lobby/<lobbyId>?secret=<player_secret>", "BAD_REQUEST")
          req.response().setStatusCode(err.statusCode).end(err.toJson)
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
                      case None =>
                        val err: ResponseErrors = ResponseErrors.NotAuthenticated
                        ws.close(err.statusCode.toShort, err.toJson)
                  case _ =>
                    val err: ResponseErrors = ResponseErrors.LobbyNotFound(LobbyId(lobbyIdStr))
                    ws.close(err.statusCode.toShort, err.toJson)
    val options = HttpServerOptions().setIdleTimeout(60)
    vertx.createHttpServer(options).requestHandler(router).listen(port)
