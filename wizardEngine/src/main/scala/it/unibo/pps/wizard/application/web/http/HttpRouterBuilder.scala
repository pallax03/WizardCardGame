package it.unibo.pps.wizard.application.web.http

import io.circe.Json
import io.vertx.core.Vertx
import io.vertx.ext.web.{Router, RoutingContext}
import io.vertx.ext.web.handler.{BodyHandler, CorsHandler, LoggerHandler, TimeoutHandler}

class HttpRouterBuilder(val vertx: Vertx, val routes: Seq[Router => Unit]):
  private val bodyLimit: Long = 1024 * 1024
  private val timeoutMs: Long = 10_000

  def build(): Router =
    val router = Router.router(vertx)
    router.route().handler(LoggerHandler.create())
    router.route().handler(BodyHandler.create().setBodyLimit(bodyLimit))
    router.route().handler(TimeoutHandler.create(timeoutMs))
    router.route().handler(CorsHandler.create().addOrigin("*"))
    routes.foreach(_(router))
    router.route().last().handler(handleNotFound)
    router.route().failureHandler(handleError)
    router

  private def handleNotFound(ctx: RoutingContext): Unit =
    val body = Json.obj("error" -> Json.fromString("Not Found")).noSpaces
    ctx.response().setStatusCode(404).putHeader("Content-Type", "application/json").end(body)

  private def handleError(ctx: RoutingContext): Unit =
    val status = if ctx.statusCode() > 0 then ctx.statusCode() else 500
    val message = Option(ctx.failure()).map(_.getMessage).getOrElse("Internal Server Error")
    val body = Json.obj("error" -> Json.fromString(message)).noSpaces
    ctx.response().setStatusCode(status).putHeader("Content-Type", "application/json").end(body)