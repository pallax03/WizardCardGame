package it.unibo.pps.wizard.application.web.http.routes

import io.circe.Json
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext

object HealthRoutes:
  def mount(router: Router): Unit =
    router.get("/health").handler(handleHealth)

  private def handleHealth(ctx: RoutingContext): Unit =
    val body = Json.obj("status" -> Json.fromString("ok")).noSpaces
    ctx
      .response()
      .putHeader("Content-Type", "application/json")
      .end(body)
