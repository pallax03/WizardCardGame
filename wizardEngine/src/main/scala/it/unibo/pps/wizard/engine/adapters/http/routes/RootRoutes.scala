package it.unibo.pps.wizard.engine.adapters.http.routes

import io.vertx.ext.web.{Router, RoutingContext}

object RootRoutes:
  def mount(router: Router): Unit =
    router.get("/").handler(handleRoot)

  private def handleRoot(ctx: RoutingContext): Unit =
    ctx.response()
      .putHeader("Content-Type", "text/plain")
      .end("Wizard engine up!")

