package io.github.pallax03.wizard.application.web.http

import scala.concurrent.Future

import io.vertx.core.AbstractVerticle
import io.vertx.ext.web.Router

import io.github.pallax03.wizard.application.web.ApiSupport
import io.github.pallax03.wizard.util.WizardLogger

import sttp.tapir.server.ServerEndpoint
import sttp.tapir.server.interceptor.log.DefaultServerLog
import sttp.tapir.server.metrics.prometheus.PrometheusMetrics
import sttp.tapir.server.vertx.{VertxFutureServerInterpreter, VertxFutureServerOptions}

/**
 * Vert.x HTTP server verticle mounting Tapir REST endpoints.
 *
 * Configures the HTTP server pipeline:
 *  - Routes: Binds declared Tapir domain endpoints, serves interactive Swagger UI documentation
 *    at `/docs` (in non-production environments), and exposes the `/metrics` Prometheus scraping endpoint.
 *  - Interceptors: Injects CORS, contextual SLF4J request logging, metrics collectors,
 *    and centralized exception handling via [[ApiSupport.exceptionHandler]].
 */
class HttpServerVerticle(
    serverEndpoints: List[ServerEndpoint[Any, Future]],
    port: Int
) extends AbstractVerticle:

  override def start(): Unit =
    val router = Router.router(vertx)

    val prometheusMetrics = PrometheusMetrics.default[Future]()

    val serverOptions = VertxFutureServerOptions.customiseInterceptors
      .corsInterceptor(sttp.tapir.server.interceptor.cors.CORSInterceptor.default[Future])
      .serverLog(serverLog)
      .exceptionHandler(ApiSupport.exceptionHandler)
      .metricsInterceptor(prometheusMetrics.metricsInterceptor())
      .options

    val interpreter = VertxFutureServerInterpreter(serverOptions)

    serverEndpoints.foreach(endpoint => interpreter.route(endpoint)(router))
    interpreter.route(prometheusMetrics.metricsEndpoint)(router)

    vertx
      .createHttpServer()
      .requestHandler(router)
      .listen(port)

  private def serverLog = DefaultServerLog[Future](
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
