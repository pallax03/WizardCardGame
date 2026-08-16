package it.unibo.pps.wizard.application.web.http

import io.vertx.core.AbstractVerticle
import io.vertx.ext.web.Router
import io.vertx.ext.web.handler.BodyHandler
import io.vertx.ext.web.handler.LoggerHandler

class HttpServerVerticle(routes: Seq[Router => Unit], port: Int) extends AbstractVerticle:

  override def start(): Unit =
    val router = Router.router(vertx)
    router.route().handler(LoggerHandler.create())
    router.route().handler(BodyHandler.create())
    routes.foreach(_(router))
    vertx.createHttpServer().requestHandler(router).listen(port)
