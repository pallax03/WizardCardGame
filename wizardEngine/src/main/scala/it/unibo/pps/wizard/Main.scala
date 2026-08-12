package it.unibo.pps.wizard

import io.vertx.core.{AbstractVerticle, Vertx}
import io.vertx.ext.web.Router
import it.unibo.pps.wizard.application.web.http.HttpServerVerticle
import it.unibo.pps.wizard.application.web.http.routes.HealthRoutes
import it.unibo.pps.wizard.application.web.http.routes.LobbyRoutes
import it.unibo.pps.wizard.application.web.http.routes.RootRoutes
import it.unibo.pps.wizard.application.web.ws.WebSocketsVerticle
import it.unibo.pps.wizard.engine.adapters.{InMemoryLobbyStatePort, RedisLobbyStateAdapter, RedisPubSubAdapter, VertxWebSocketsAdapter}

object Main:
  val httpPort: Int = sys.env.getOrElse("HTTP_PORT", "8080").toInt
  val wsPort: Int = sys.env.getOrElse("PORT", "8081").toInt

  def main(args: Array[String]): Unit =
    val vertx = Vertx.vertx()
    runHTTPServer(vertx)
    runWSServer(vertx)

  private def runHTTPServer(vertx: Vertx): Unit =
    // todo modify into redis when fully working
    //val lobbyStatePort = RedisLobbyStateAdapter(redisAPI)
    val lobbyStatePort = InMemoryLobbyStatePort()
    val routes: Seq[Router => Unit] = Seq(
      RootRoutes.mount,
      HealthRoutes.mount,
      LobbyRoutes(lobbyStatePort).mount
    )
    val verticle = HttpServerVerticle(routes, httpPort)
    deploy(vertx, verticle, "HTTP", httpPort)

  private def runWSServer(vertx: Vertx): Unit =
    val lobbyStatePort = RedisLobbyStateAdapter()
    val redisPubSub = RedisPubSubAdapter()
    val wsAdapter = VertxWebSocketsAdapter(redisPubSub)
    val verticle = WebSocketsVerticle(wsAdapter, lobbyStatePort, wsPort)
    deploy(vertx, verticle, "WebSocket", wsPort)

  private def deploy(vertx: Vertx,  verticle: AbstractVerticle, name: String, port: Int): Unit =
    vertx
      .deployVerticle(verticle)
      .onComplete: ar =>
        if ar.succeeded() then println(s"$name server deployed ($ar) on port $port")
        else println(s"$name Deploy failed: ${ar.cause().getMessage}")