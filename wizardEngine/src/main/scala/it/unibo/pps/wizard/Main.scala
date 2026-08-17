package it.unibo.pps.wizard

import io.vertx.core.AbstractVerticle
import io.vertx.core.Vertx
import io.vertx.redis.client.Redis
import io.vertx.redis.client.RedisOptions
import it.unibo.pps.wizard.application.bot.BotManagerVerticle
import it.unibo.pps.wizard.application.web.http.HttpServerVerticle
import it.unibo.pps.wizard.application.web.http.routes.*
import it.unibo.pps.wizard.application.web.ws.WebSocketsVerticle
import it.unibo.pps.wizard.engine.adapters.VertxWebSocketsAdapter
import it.unibo.pps.wizard.engine.adapters.prolog.WizardPrologAdapter
import it.unibo.pps.wizard.engine.adapters.redis.RedisInboundAdapter
import it.unibo.pps.wizard.engine.adapters.redis.RedisLobbyStateAdapter
import it.unibo.pps.wizard.engine.adapters.redis.RedisOutboundAdapter
import it.unibo.pps.wizard.engine.adapters.redis.RedisPubSubAdapter
import it.unibo.pps.wizard.engine.ports.{AIPort, InboundPort, LobbyStatePort, OutboundPort, PubSubPort}

import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContext.Implicits.global

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

    deploy(
      vertx,
      BotManagerVerticle(pubSubPort, prologPort, lobbyStatePort, inPort),
      "bot verticle",
      0
    )
    runHTTPServer(vertx, inPort, lobbyStatePort, prologPort)
    runWSServer(vertx, lobbyStatePort, pubSubPort)

  private def runHTTPServer(
      vertx: Vertx,
      gameEngineInPort: InboundPort,
      lobbyStatePort: LobbyStatePort,
      prologPort: AIPort
  )(using ec: ExecutionContext): Unit =
    val lobbyRoutes = LobbyRoutes(lobbyStatePort, gameEngineInPort)
    val actionRoutes = ActionRoutes(lobbyStatePort, gameEngineInPort)
    val aiRoutes = AIRoutes(lobbyStatePort, prologPort)
    val allEndpoints = lobbyRoutes.all ++ actionRoutes.all ++ aiRoutes.all
    val verticle = HttpServerVerticle(allEndpoints, httpPort)
    deploy(vertx, verticle, "HTTP", httpPort)

  private def runWSServer(
      vertx: Vertx,
      lobbyStatePort: LobbyStatePort,
      pubSubPort: PubSubPort
  ): Unit =
    val wsAdapter = VertxWebSocketsAdapter(pubSubPort)
    val verticle = WebSocketsVerticle(wsAdapter, lobbyStatePort, wsPort)
    deploy(vertx, verticle, "WebSocket", wsPort)

  private def deploy(vertx: Vertx, verticle: AbstractVerticle, name: String, port: Int): Unit =
    vertx
      .deployVerticle(verticle)
      .onComplete: ar =>
        if ar.succeeded() then println(s"$name server deployed on port $port")
        else println(s"$name Deploy failed: ${ar.cause().getMessage}")
