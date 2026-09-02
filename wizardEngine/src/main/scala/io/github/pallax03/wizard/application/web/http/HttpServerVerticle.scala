package io.github.pallax03.wizard.application.web.http

import io.circe.generic.auto._
import io.github.pallax03.wizard.application.web.http.endpoints.ErrorResponse
import io.github.pallax03.wizard.engine.model.core.GameException
import io.github.pallax03.wizard.util.WizardLogger
import io.vertx.core.AbstractVerticle
import io.vertx.ext.web.Router
import sttp.tapir._
import sttp.tapir.generic.auto._
import sttp.tapir.json.circe._
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.server.interceptor.exception.ExceptionHandler
import sttp.tapir.server.interceptor.log.DefaultServerLog
import sttp.tapir.server.model.ValuedEndpointOutput
import sttp.tapir.server.vertx.VertxFutureServerInterpreter
import sttp.tapir.server.vertx.VertxFutureServerOptions
import sttp.tapir.swagger.bundle.SwaggerInterpreter

import scala.concurrent.Future

class HttpServerVerticle(serverEndpoints: List[ServerEndpoint[Any, Future]], port: Int)
    extends AbstractVerticle:

  override def start(): Unit =
    val router = Router.router(vertx)

    val serverLog = DefaultServerLog[Future](
      doLogWhenReceived = msg => Future.successful(WizardLogger.info(msg)),
      doLogWhenHandled = (msg, error) =>
        Future.successful(error.fold(WizardLogger.info(msg))(err => WizardLogger.error(msg, err))),
      doLogAllDecodeFailures = (msg, error) =>
        Future.successful(
          error.fold(WizardLogger.warn(msg))(err => WizardLogger.warn(s"$msg: $err"))
        ),
      doLogExceptions = (msg, ex) => Future.successful(WizardLogger.error(msg, ex)),
      noLog = Future.successful(())
    )

    import io.github.pallax03.wizard.application.web._

    val exceptionHandler = ExceptionHandler.pure[Future](ctx =>
      val lobbyIdOpt = ctx.request.extractLobbyIdStr
      val playerIdOpt = ctx.request.extractPlayerIdStr

      WizardLogger.withContext(lobbyIdOpt, playerIdOpt) {
        ctx.e match
          case ge: GameException =>
            WizardLogger.error(s"GameException interceptada sull'endpoint ${ctx.endpoint.show}", ge)
            Some(
              ValuedEndpointOutput(
                jsonBody[ErrorResponse].and(
                  sttp.tapir.statusCode(sttp.model.StatusCode.InternalServerError)
                ),
                ErrorResponse(ge.getMessage, "GAME_EXCEPTION")
              )
            )
          case ex: Throwable =>
            WizardLogger.error(s"Errore non gestito sull'endpoint ${ctx.endpoint.show}", ex)
            Some(
              ValuedEndpointOutput(
                jsonBody[ErrorResponse].and(
                  sttp.tapir.statusCode(sttp.model.StatusCode.InternalServerError)
                ),
                ErrorResponse("Internal Server Error", "INTERNAL_ERROR")
              )
            )
      }
    )

    val serverOptions = VertxFutureServerOptions.customiseInterceptors
      .serverLog(serverLog)
      .exceptionHandler(exceptionHandler)
      .options

    val isProduction = sys.env.getOrElse("APP_ENV", "development") == "production"
    val swaggerEndpoints =
      if (!isProduction)
        SwaggerInterpreter().fromServerEndpoints(serverEndpoints, "Wizard Game Engine API", "1.0.0")
      else List.empty

    val allEndpoints = serverEndpoints ++ swaggerEndpoints

    val interpreter = VertxFutureServerInterpreter(serverOptions)
    allEndpoints.foreach(endpoint => interpreter.route(endpoint)(router))

    vertx
      .createHttpServer()
      .requestHandler(router)
      .listen(port)
