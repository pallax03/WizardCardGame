package io.github.pallax03.wizard

import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContext.Implicits.global

import io.vertx.core.{AbstractVerticle, Vertx}
import io.vertx.redis.client.{Redis, RedisOptions}

import io.github.pallax03.wizard.application.bot.BotManagerVerticle
import io.github.pallax03.wizard.application.web.http.HttpServerVerticle
import io.github.pallax03.wizard.application.web.http.routes._
import io.github.pallax03.wizard.application.web.ws.WebSocketsVerticle
import io.github.pallax03.wizard.engine.adapters.VertxWebSocketsAdapter
import io.github.pallax03.wizard.engine.adapters.prolog.WizardPrologAdapter
import io.github.pallax03.wizard.engine.adapters.redis._
import io.github.pallax03.wizard.engine.ports._

import sttp.tapir.swagger.bundle.SwaggerInterpreter

object Main:
  private val httpPort: Int = sys.env.getOrElse("HTTP_PORT", "5001").toInt
  private val wsPort: Int = sys.env.getOrElse("WS_PORT", "5002").toInt
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
    val recoveryPort: GameRecoveryPort = RedisGameRecoveryAdapter(redisClient, lobbyStatePort, outPort)
    val inPort: InboundPort = RedisInboundAdapter(redisClient, outPort, recoveryPort)
    val prologPort = WizardPrologAdapter(inPort)

    deploy(
      vertx,
      BotManagerVerticle(pubSubPort, prologPort, lobbyStatePort, inPort),
      "bot verticle",
      0
    )
    runHTTPServer(vertx, inPort, lobbyStatePort, prologPort)
    runWSServer(vertx, lobbyStatePort, pubSubPort)

  private def isProduction: Boolean =
    sys.env.getOrElse("APP_ENV", "development").toLowerCase == "production"

  private def runHTTPServer(
      vertx: Vertx,
      gameEngineInPort: InboundPort,
      lobbyStatePort: LobbyStatePort,
      prologPort: AIPort
  )(using ec: ExecutionContext): Unit =
    val lobbyRoutes = LobbyRoutes(lobbyStatePort, gameEngineInPort)
    val actionRoutes = ActionRoutes(lobbyStatePort, gameEngineInPort)
    val aiRoutes = AIRoutes(lobbyStatePort, prologPort)
    val domainEndpoints = lobbyRoutes.all ++ actionRoutes.all ++ aiRoutes.all

    val swaggerEndpoints = if !isProduction then SwaggerInterpreter().fromServerEndpoints(domainEndpoints, "Wizard Game Engine API", "1.0.0") else List.empty

    val allEndpoints = domainEndpoints ++ swaggerEndpoints
    val verticle = HttpServerVerticle(allEndpoints, httpPort)
    deploy(vertx, verticle, "HTTP", httpPort)

  private def runWSServer(
      vertx: Vertx,
      lobbyStatePort: LobbyStatePort,
      pubSubPort: PubSubPort
  ): Unit =
    val wsAdapter = VertxWebSocketsAdapter(pubSubPort, lobbyStatePort)
    val verticle = WebSocketsVerticle(wsAdapter, lobbyStatePort, wsPort)
    deploy(vertx, verticle, "WebSocket", wsPort)

  private def deploy(vertx: Vertx, verticle: AbstractVerticle, name: String, port: Int): Unit =
    vertx
      .deployVerticle(verticle)
      .onComplete: ar =>
        if ar.succeeded() then println(s"$name server deployed on port $port")
        else println(s"$name Deploy failed: ${ar.cause().getMessage}")
