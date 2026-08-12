package it.unibo.pps.wizard

import io.vertx.core.{AbstractVerticle, Vertx}
import io.vertx.ext.web.Router
import it.unibo.pps.wizard.application.web.http.HttpServerVerticle
import it.unibo.pps.wizard.application.web.http.routes._
import it.unibo.pps.wizard.application.web.ws.WebSocketsVerticle
import it.unibo.pps.wizard.engine.adapters.LocalGameInboundAdapter
import it.unibo.pps.wizard.engine.adapters.redis.{RedisGameEngineOutboundAdapter, RedisLobbyStateAdapter, RedisPubSubAdapter}
import it.unibo.pps.wizard.engine.adapters.VertxWebSocketsAdapter
import it.unibo.pps.wizard.engine.ports.{GameEngineInboundPort, GameEngineOutboundPort, LobbyStatePort, PubSubPort}
import io.vertx.redis.client.{Redis, RedisOptions}

object Main:
  val httpPort: Int = sys.env.getOrElse("HTTP_PORT", "8080").toInt
  val wsPort: Int = sys.env.getOrElse("WS_PORT", "8081").toInt

  def main(args: Array[String]): Unit =
    val vertx = Vertx.vertx()

    println("Starting Redis...")
    val redisHost = sys.env.getOrElse("REDIS_HOST", "localhost")
    val redisPortStr = sys.env.getOrElse("REDIS_PORT", "6379")
    val redisOptions = RedisOptions().setConnectionString(s"redis://$redisHost:$redisPortStr")
    val redisClient = Redis.createClient(vertx, redisOptions)
    
    val pubSubPort: PubSubPort = RedisPubSubAdapter(redisClient)
    val lobbyStatePort: LobbyStatePort = RedisLobbyStateAdapter(redisClient)
    
    val gameEngineOutPort: GameEngineOutboundPort = RedisGameEngineOutboundAdapter(pubSubPort)
    val gameEngineInPort: GameEngineInboundPort = LocalGameInboundAdapter(vertx, gameEngineOutPort)
    
    runHTTPServer(vertx, gameEngineInPort, lobbyStatePort)
    runWSServer(vertx, lobbyStatePort, pubSubPort)

  private def runHTTPServer(vertx: Vertx, gameEngineInPort: GameEngineInboundPort, lobbyStatePort: LobbyStatePort): Unit =
    val routes: Seq[Router => Unit] = Seq(
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
        if ar.succeeded() then println(s"$name server deployed on port $port")
        else println(s"$name Deploy failed: ${ar.cause().getMessage}")