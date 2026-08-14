package it.unibo.pps.wizard

import io.vertx.core.{AbstractVerticle, Vertx}
import io.vertx.ext.web.Router
import it.unibo.pps.wizard.application.web.http.HttpServerVerticle
import it.unibo.pps.wizard.application.web.http.routes.*
import it.unibo.pps.wizard.application.web.ws.WebSocketsVerticle
import it.unibo.pps.wizard.engine.adapters.VertxWebSocketsAdapter
import it.unibo.pps.wizard.engine.ports.{InboundPort, LobbyStatePort, OutboundPort, PubSubPort}
import io.vertx.redis.client.{Redis, RedisOptions}
import it.unibo.pps.wizard.application.bot.BotManagerVerticle
import it.unibo.pps.wizard.engine.adapters.prolog.WizardPrologAdapter
import it.unibo.pps.wizard.engine.adapters.redis.{RedisInboundAdapter, RedisLobbyStateAdapter, RedisOutboundAdapter, RedisPubSubAdapter}

object Main:
  private val httpPort: Int = sys.env.getOrElse("HTTP_PORT", "8080").toInt
  private val wsPort: Int = sys.env.getOrElse("WS_PORT", "8081").toInt
  private val redisHost = sys.env.getOrElse("REDIS_HOST", "localhost")
  private val redisPort = sys.env.getOrElse("REDIS_PORT", "6379").toInt
  private val redisPoolSize = sys.env.getOrElse("REDIS_POOL_SIZE", "6").toInt

  def main(args: Array[String]): Unit =
    val vertx = Vertx.vertx()

    val redisOptions = RedisOptions()
      .setConnectionString(s"redis://$redisHost:$redisPort")
      .setMaxPoolSize(redisPoolSize)
    val redisClient = Redis.createClient(vertx, redisOptions)
    
    val pubSubPort: PubSubPort = RedisPubSubAdapter(redisClient)
    val lobbyStatePort: LobbyStatePort = RedisLobbyStateAdapter(redisClient)
    
    val outPort: OutboundPort = RedisOutboundAdapter(pubSubPort)
    val inPort: InboundPort = RedisInboundAdapter(redisClient, outPort)
    val prologPort = WizardPrologAdapter(inPort)

    deploy(vertx, BotManagerVerticle(pubSubPort, prologPort, lobbyStatePort, inPort), "bot verticle", 0)
    runHTTPServer(vertx, inPort, lobbyStatePort)
    runWSServer(vertx, lobbyStatePort, pubSubPort)

  private def runHTTPServer(vertx: Vertx, gameEngineInPort: InboundPort, lobbyStatePort: LobbyStatePort): Unit =
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