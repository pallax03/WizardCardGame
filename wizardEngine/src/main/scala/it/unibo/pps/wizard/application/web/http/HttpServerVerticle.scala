package it.unibo.pps.wizard.application.web.http

import io.vertx.core.AbstractVerticle
import io.vertx.ext.web.Router
import io.vertx.ext.web.handler.LoggerHandler
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.server.vertx.VertxFutureServerInterpreter
import sttp.tapir.swagger.bundle.SwaggerInterpreter

import scala.concurrent.Future

class HttpServerVerticle(endpoints: List[ServerEndpoint[Any, Future]], port: Int) extends AbstractVerticle:

  override def start(): Unit =
    val router = Router.router(vertx)
    router.route().handler(LoggerHandler.create())
    val swaggerEndpoints = SwaggerInterpreter().fromServerEndpoints(
      endpoints,
      "Wizard Game Engine API",
      "1.0.0"
    )
    val allEndpoints = endpoints ++ swaggerEndpoints
    val interpreter = VertxFutureServerInterpreter()
    allEndpoints.foreach(endpoint => interpreter.route(endpoint)(router))
    vertx.createHttpServer()
      .requestHandler(router)
      .listen(port)