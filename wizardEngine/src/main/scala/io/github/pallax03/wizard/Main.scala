package io.github.pallax03.wizard

import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContext.Implicits.global

import io.vertx.core.{AbstractVerticle, Vertx}
import io.vertx.redis.client.{ProtocolVersion, Redis, RedisOptions}

import io.github.pallax03.wizard.application.bot.BotManagerVerticle
import io.github.pallax03.wizard.application.logging.PubSubLoggerVerticle
import io.github.pallax03.wizard.application.timer.TurnTimerVerticle
import io.github.pallax03.wizard.application.web.http.HttpServerVerticle
import io.github.pallax03.wizard.application.web.http.routes.*
import io.github.pallax03.wizard.application.web.ws.WebSocketsVerticle
import io.github.pallax03.wizard.engine.adapters.VertxWebSocketsAdapter
import io.github.pallax03.wizard.engine.adapters.prolog.WizardPrologAdapter
import io.github.pallax03.wizard.engine.adapters.redis.*
import io.github.pallax03.wizard.engine.ports.*

import sttp.tapir.swagger.bundle.SwaggerInterpreter

/**
 * Application entry point.
 *
 * The process role is controlled by the `ROLE` environment variable:
 *
 *  - `engine`     (default) → deploys HTTP, WebSocket, TurnTimer and Logger verticles.
 *  - `bot_worker`           → deploys only the BotManagerVerticle.
 *                             Useful to run bot workers as independent Docker replicas
 *                             or locally from a terminal while the engine runs in IntelliJ.
 *
 * Both roles share the same JAR; Docker simply passes a different `ROLE` env var.
 */
object Main:
  private val httpPort: Int = sys.env.getOrElse("HTTP_PORT", "5001").toInt
  private val wsPort: Int = sys.env.getOrElse("WS_PORT", "5002").toInt
  private val redisHost: String = sys.env.getOrElse("REDIS_HOST", "localhost")
  private val redisPort: Int = sys.env.getOrElse("REDIS_PORT", "6379").toInt
  private val redisPoolSize: Int = sys.env.getOrElse("REDIS_POOL_SIZE", "6").toInt
  private val role: String = sys.env.getOrElse("ROLE", "engine").toLowerCase

  def main(args: Array[String]): Unit =
    val vertx = Vertx.vertx()

    val redisOptions = RedisOptions()
      .setConnectionString(s"redis://$redisHost:$redisPort")
      .setMaxPoolSize(redisPoolSize)
      .setPreferredProtocolVersion(ProtocolVersion.RESP2)
    val redisClient = Redis.createClient(vertx, redisOptions)

    val pubSubPort: PubSubPort = RedisPubSubAdapter(redisClient)
    val lobbyStatePort: LobbyStatePort = RedisLobbyStateAdapter(redisClient)
    val outPort: OutboundPort = RedisOutboundAdapter(pubSubPort, redisClient, lobbyStatePort)
    val recoveryPort: GameRecoveryPort = RedisGameRecoveryAdapter(redisClient, outPort, pubSubPort)
    val inPort: InboundPort = RedisInboundAdapter(redisClient, outPort, recoveryPort)
    val prologPort = WizardPrologAdapter(inPort)

    role match
      case "bot_worker" =>
        println(
          s"[Main] Starting as BOT WORKER (redis=$redisHost:$redisPort)"
        )
        deploy(
          vertx,
          BotManagerVerticle(
            pubSubPort,
            prologPort,
            lobbyStatePort,
            inPort,
            redisClient
          ),
          "bot worker",
          0
        )
        deploy(vertx, PubSubLoggerVerticle(pubSubPort), "pubsub logger", 0)

      case _ =>
        println(
          s"[Main] Starting as ENGINE (http=$httpPort, ws=$wsPort, redis=$redisHost:$redisPort)"
        )
        deploy(vertx, PubSubLoggerVerticle(pubSubPort), "pubsub logger", 0)
        deploy(
          vertx,
          TurnTimerVerticle(pubSubPort, redisClient, inPort, lobbyStatePort),
          "turn timer",
          0
        )
        runHTTPServer(vertx, inPort, lobbyStatePort, prologPort)
        runWSServer(vertx, inPort, lobbyStatePort, pubSubPort)

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

    val swaggerEndpoints =
      if !isProduction then
        SwaggerInterpreter().fromServerEndpoints(domainEndpoints, "Wizard Game Engine API", "1.0.0")
      else List.empty

    val allEndpoints = domainEndpoints ++ swaggerEndpoints
    deploy(vertx, HttpServerVerticle(allEndpoints, httpPort), "HTTP", httpPort)

  private def runWSServer(
      vertx: Vertx,
      inPort: InboundPort,
      lobbyStatePort: LobbyStatePort,
      pubSubPort: PubSubPort
  ): Unit =
    val wsAdapter = VertxWebSocketsAdapter(vertx, pubSubPort, lobbyStatePort, inPort)
    deploy(vertx, WebSocketsVerticle(wsAdapter, lobbyStatePort, wsPort), "WebSocket", wsPort)

  private def deploy(vertx: Vertx, verticle: AbstractVerticle, name: String, port: Int): Unit =
    vertx
      .deployVerticle(verticle)
      .onComplete: ar =>
        if ar.succeeded() then println(s"[$name] deployed (port=$port)")
        else println(s"[$name] Deploy FAILED: ${ar.cause().getMessage}")
