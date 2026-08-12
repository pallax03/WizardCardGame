package it.unibo.pps.wizard

import io.vertx.core.{AbstractVerticle, Vertx}
import io.vertx.ext.web.Router
import it.unibo.pps.wizard.application.web.http.HttpServerVerticle
import it.unibo.pps.wizard.application.web.http.routes.{ActionRoutes, HealthRoutes, LobbyRoutes, RootRoutes}
import it.unibo.pps.wizard.application.web.ws.WebSocketsVerticle
import it.unibo.pps.wizard.engine.adapters.inmemory.{LocalGameInboundAdapter, LocalLobbyStatePort}
import it.unibo.pps.wizard.engine.adapters.redis.{RedisGameEngineOutboundAdapter, RedisLobbyStateAdapter, RedisPubSubAdapter}
import it.unibo.pps.wizard.engine.adapters.VertxWebSocketsAdapter
import it.unibo.pps.wizard.engine.lobby.LobbyId
import it.unibo.pps.wizard.engine.ports.{GameEngineInboundPort, GameEngineOutboundPort}

object Main:
  val httpPort: Int = sys.env.getOrElse("HTTP_PORT", "8080").toInt
  val wsPort: Int = sys.env.getOrElse("PORT", "8081").toInt

  def main(args: Array[String]): Unit =
    val vertx = Vertx.vertx()
    val redisAPI = RedisPubSubAdapter()
    val gameEngineOutPort: GameEngineOutboundPort = RedisGameEngineOutboundAdapter(LobbyId("0"), redisAPI)
    val gameEngineInPort: GameEngineInboundPort = LocalGameInboundAdapter(vertx, gameEngineOutPort)
    runHTTPServer(vertx, gameEngineInPort)
    runWSServer(vertx)

  private def runHTTPServer(vertx: Vertx, gameEngineInPort: GameEngineInboundPort): Unit =
    // todo modify into redis when fully working
    //val lobbyStatePort = RedisLobbyStateAdapter(redisAPI)
    val lobbyStatePort = LocalLobbyStatePort()
    val routes: Seq[Router => Unit] = Seq(
      RootRoutes.mount,
      HealthRoutes.mount,
      LobbyRoutes(lobbyStatePort, gameEngineInPort).mount,
      ActionRoutes(lobbyStatePort, gameEngineInPort).mount
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