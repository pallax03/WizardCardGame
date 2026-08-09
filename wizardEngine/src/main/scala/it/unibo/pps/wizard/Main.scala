package it.unibo.pps.wizard

import io.vertx.core.DeploymentOptions
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.Router
import it.unibo.pps.wizard.application.web.http.HttpServerVerticle
import it.unibo.pps.wizard.application.web.http.routes.HealthRoutes
import it.unibo.pps.wizard.application.web.http.routes.RootRoutes
import it.unibo.pps.wizard.application.web.ws.WebSocketsVerticle
import it.unibo.pps.wizard.engine.adapters.RedisLobbyStateAdapter
import it.unibo.pps.wizard.engine.adapters.RedisPubSubAdapter
import it.unibo.pps.wizard.engine.adapters.VertxWebSocketsAdapter

import scala.annotation.nowarn

object Main:
  val port: Int = sys.env.getOrElse("PORT", "8080").toInt

  def main(args: Array[String]): Unit =
    val vertx = Vertx.vertx()
//    runHTTPServer(vertx)
    runWSServer(vertx)

  @nowarn
  private def runHTTPServer(vertx: Vertx): Unit =
    val routes: Seq[Router => Unit] = Seq(RootRoutes.mount, HealthRoutes.mount)
    val options = DeploymentOptions().setConfig(JsonObject().put("http.port", port))

    vertx
      .deployVerticle(HttpServerVerticle(routes), options)
      .onComplete: ar =>
        if (ar.succeeded()) println(s"HTTP server deployed ($ar) on port $port")
        else
          println(s"Deploy failed: ${ar.cause().getMessage}")
          vertx.close()

  private def runWSServer(vertx: Vertx): Unit =
    val lobbyStatePort = RedisLobbyStateAdapter()
    val redisPubSub = RedisPubSubAdapter()
    val wsAdapter = VertxWebSocketsAdapter(redisPubSub)
    vertx
      .deployVerticle(WebSocketsVerticle(wsAdapter, lobbyStatePort, port))
      .onComplete: ar =>
        if ar.succeeded() then println(s"WebSocket server deployed ($ar) on port $port")
        else println(s"WebSocket Deploy failed: ${ar.cause().getMessage}")
