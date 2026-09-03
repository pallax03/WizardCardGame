package io.github.pallax03.wizard.application.web.http

import scala.concurrent.Future

import io.circe.generic.auto._

import io.vertx.core.AbstractVerticle
import io.vertx.ext.web.Router

import io.github.pallax03.wizard.application.web._
import io.github.pallax03.wizard.application.web.http.endpoints.ErrorResponse
import io.github.pallax03.wizard.engine.model.core.{AbortedGameException, GameException, RecoveredGameException}
import io.github.pallax03.wizard.util.WizardLogger

import sttp.model.StatusCode
import sttp.tapir._
import sttp.tapir.generic.auto._
import sttp.tapir.json.circe._
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.server.interceptor.exception.ExceptionHandler
import sttp.tapir.server.interceptor.log.DefaultServerLog
import sttp.tapir.server.model.ValuedEndpointOutput
import sttp.tapir.server.vertx.{VertxFutureServerInterpreter, VertxFutureServerOptions}

class HttpServerVerticle(
    serverEndpoints: List[ServerEndpoint[Any, Future]],
    port: Int
) extends AbstractVerticle:

  override def start(): Unit =
    val router = Router.router(vertx)

    val serverOptions = VertxFutureServerOptions.customiseInterceptors
      .serverLog(serverLog)
      .exceptionHandler(exceptionHandler)
      .options

    val interpreter = VertxFutureServerInterpreter(serverOptions)
    serverEndpoints.foreach(endpoint => interpreter.route(endpoint)(router))

    vertx
      .createHttpServer()
      .requestHandler(router)
      .listen(port)

  private def serverLog = DefaultServerLog[Future](
    doLogWhenReceived = msg => Future.successful(WizardLogger.info(msg)),
    doLogWhenHandled = (msg, error) =>
      Future.successful(error.fold(WizardLogger.info(msg))(err => WizardLogger.error(msg, err))),
    doLogAllDecodeFailures = (msg, error) =>
      Future.successful(error.fold(WizardLogger.warn(msg))(err => WizardLogger.warn(s"$msg: $err"))),
    doLogExceptions = (msg, ex) => Future.successful(WizardLogger.error(msg, ex)),
    noLog = Future.successful(())
  )

  private def exceptionHandler = ExceptionHandler[Future](ctx =>
    val lobbyIdOpt = ctx.request.extractLobbyIdStr
    val playerIdOpt = ctx.request.extractPlayerIdStr

    WizardLogger.withContext(lobbyIdOpt, playerIdOpt):
      val (logMsg, clientMsg, code) = ctx.e match
        case rge: RecoveredGameException =>
          (s"RECOVERED GameException (${rge.ge}), endpoint: ${ctx.endpoint.show}", rge.ge.getMessage, rge.getMessage)
        case age: AbortedGameException =>
          (s"ABORTED GameException (${age.ge}), endpoint: ${ctx.endpoint.show}", age.ge.getMessage, age.getMessage)
        case ge: GameException =>
          (s"NOT HANDLED GameException ${ctx.endpoint.show}", ge.getMessage, "GAME_EXCEPTION")
        case _ =>
          (s"CRASH ${ctx.endpoint.show}", "Internal Server Error", "INTERNAL_ERROR")

      WizardLogger.error(logMsg, ctx.e)
      val errorOutput = jsonBody[ErrorResponse].and(statusCode(StatusCode.InternalServerError))
      Future.successful(Some(ValuedEndpointOutput(errorOutput, ErrorResponse(clientMsg, code))))
  )
