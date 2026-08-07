package it.unibo.pps.wizard.engine.adapters.http

import io.vertx.core.AbstractVerticle
import io.vertx.core.http.{HttpServer, HttpServerOptions}
import io.vertx.ext.web.Router

class HttpServerVerticle(routes: Seq[Router => Unit]) extends AbstractVerticle:
  private var httpServer: HttpServer | Null = null

  override def start(): Unit =
    val port = config().getInteger("http.port").intValue()
    val router = HttpRouterBuilder(vertx, routes).build()
    val options = HttpServerOptions().setHost("0.0.0.0")
    httpServer = vertx.createHttpServer(options).requestHandler(router)
    httpServer.listen(port).onComplete: ar =>
      if (ar.succeeded())
        println(s"HTTP server listening on port $port")
      else
        println(s"Failed to start HTTP server on port $port: ${ar.cause().getMessage}")
        vertx.close()

  override def stop(): Unit =
    if httpServer != null then httpServer.close()