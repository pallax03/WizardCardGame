package it.unibo.pps.wizard

import io.vertx.core.{AbstractVerticle, Vertx}
import io.vertx.ext.web.Router
import it.unibo.pps.wizard.application.web.http.HttpServerVerticle
import it.unibo.pps.wizard.application.web.http.routes.{ActionRoutes, HealthRoutes, LobbyRoutes, RootRoutes}
import it.unibo.pps.wizard.application.web.ws.WebSocketsVerticle
import it.unibo.pps.wizard.engine.adapters.inmemory.{LocalGameInboundAdapter, LocalLobbyStatePort, LocalPubSubAdapter, LocalGameOutboundAdapter}
import it.unibo.pps.wizard.engine.adapters.redis.{RedisGameEngineOutboundAdapter, RedisLobbyStateAdapter, RedisPubSubAdapter}
import it.unibo.pps.wizard.engine.adapters.VertxWebSocketsAdapter
import it.unibo.pps.wizard.engine.ports.{GameEngineInboundPort, GameEngineOutboundPort, LobbyStatePort, PubSubPort}

object Main:
  val httpPort: Int = sys.env.getOrElse("HTTP_PORT", "8080").toInt
  val wsPort: Int = sys.env.getOrElse("WS_PORT", "8081").toInt

  def main(args: Array[String]): Unit =
    val vertx = Vertx.vertx()
    val useRedis = args.contains("-redis")

    val (pubSubPort: PubSubPort, lobbyStatePort: LobbyStatePort) = if useRedis then
      println("Starting Redis...")
      (RedisPubSubAdapter(), RedisLobbyStateAdapter())
    else
      println("Starting In-Memory...")
      (LocalPubSubAdapter(vertx), LocalLobbyStatePort())

    val gameEngineOutPort: GameEngineOutboundPort = if useRedis then
      RedisGameEngineOutboundAdapter(pubSubPort)
    else
      LocalGameOutboundAdapter(pubSubPort)

    val gameEngineInPort: GameEngineInboundPort = LocalGameInboundAdapter(vertx, gameEngineOutPort)
    
    runHTTPServer(vertx, gameEngineInPort, lobbyStatePort)
    runWSServer(vertx, lobbyStatePort, pubSubPort)

  private def runHTTPServer(vertx: Vertx, gameEngineInPort: GameEngineInboundPort, lobbyStatePort: LobbyStatePort): Unit =
    val routes: Seq[Router => Unit] = Seq(
      RootRoutes.mount,
      HealthRoutes.mount,
      LobbyRoutes(lobbyStatePort, gameEngineInPort).mount,
      ActionRoutes(lobbyStatePort, gameEngineInPort).mount
    )
    val verticle = HttpServerVerticle(routes, httpPort)
    deploy(vertx, verticle, "HTTP", httpPort)

  private def runWSServer(vertx: Vertx, lobbyStatePort: LobbyStatePort, pubSubPort: PubSubPort): Unit =
    val wsAdapter = VertxWebSocketsAdapter(pubSubPort)
    val verticle = WebSocketsVerticle(wsAdapter, lobbyStatePort, wsPort)
    deploy(vertx, verticle, "WebSocket", wsPort)

  private def deploy(vertx: Vertx,  verticle: AbstractVerticle, name: String, port: Int): Unit =
    vertx
      .deployVerticle(verticle)
      .onComplete: ar =>
        if ar.succeeded() then println(s"$name server deployed ($ar) on port $port")
        else println(s"$name Deploy failed: ${ar.cause().getMessage}")