package io.github.pallax03.wizard.application.web.ws

import scala.util.Success

import io.vertx.core.AbstractVerticle
import io.vertx.core.http.HttpServerOptions
import io.vertx.ext.web.{Router, RoutingContext}

import io.github.pallax03.wizard.codecs.engine.lobby.LobbyCodecs.given
import io.github.pallax03.wizard.codecs.syntax.CodecSyntax.toJson
import io.github.pallax03.wizard.engine.lobby.LobbyId
import io.github.pallax03.wizard.engine.ports.{LobbyStatePort, WebSocketsPort}
import io.github.pallax03.wizard.util.ApiSupport
import io.github.pallax03.wizard.util.FutureSyntax.*

import sttp.model.StatusCode

/**
 * Vert.x HTTP server verticle hosting the real-time WebSocket gateway.
 *
 *  - Authenticates the player against [[LobbyStatePort.getAuthLobby]].
 *  - On success: Upgrades connection to WebSocket and invokes [[WebSocketsPort.subscribeToLobbyEvents]].
 *  - On failure: Closes socket with the status code mapped via [[ApiSupport.statusCodeFor]].
 *  - Enforces a 60-second idle timeout.
 */
class WebSocketsVerticle(
    wsPortAdapter: WebSocketsPort,
    lobbyStatePort: LobbyStatePort,
    port: Int
) extends AbstractVerticle:

  private val WS_IDLE_TIMEOUT_SECONDS: Int = 60

  override def start(): Unit =
    val router = Router.router(vertx)
    router
      .route(WebSocketEndpoints.vertxRoutePath)
      .handler: ctx =>
        val req = ctx.request()
        (
          Option(req.getParam(ApiSupport.LobbyIdParam)),
          Option(req.getParam(WebSocketEndpoints.SecretParam))
        ) match
          case (Some(lobbyIdStr), Some(secret)) =>
            upgradeAndSubscribe(ctx, LobbyId(lobbyIdStr), secret)
          case _ =>
            req.response().setStatusCode(StatusCode.BadRequest.code).end()

    val options = HttpServerOptions().setIdleTimeout(WS_IDLE_TIMEOUT_SECONDS)
    vertx.createHttpServer(options).requestHandler(router).listen(port)

  private def upgradeAndSubscribe(
      ctx: RoutingContext,
      lobbyId: LobbyId,
      secret: String
  ): Unit =
    ctx
      .request()
      .toWebSocket
      .onComplete: res =>
        if res.succeeded() then
          val ws = res.result()
          lobbyStatePort
            .getAuthLobby(lobbyId, secret)
            .onVertxComplete(ctx):
              case Success(Right((player, _))) =>
                wsPortAdapter.subscribeToLobbyEvents(lobbyId, player.id, ws)
              case Success(Left(err)) =>
                ws.close(ApiSupport.statusCodeFor(err).code.toShort, err.toJson)
              case _ =>
                ws.close(StatusCode.InternalServerError.code.toShort, "Internal Server Error")
