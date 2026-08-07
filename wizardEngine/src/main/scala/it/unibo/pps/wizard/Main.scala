package it.unibo.pps.wizard

import io.vertx.core.json.JsonObject
import io.vertx.core.{DeploymentOptions, Vertx}
import io.vertx.ext.web.Router
import it.unibo.pps.wizard.engine.adapters.http.HttpServerVerticle
import it.unibo.pps.wizard.engine.adapters.http.routes.{HealthRoutes, RootRoutes}

object Main:

  def main(args: Array[String]): Unit =
    val port = sys.env.getOrElse("PORT", "8080").toInt
    val vertx = Vertx.vertx()
    val routes: Seq[Router => Unit] = Seq(RootRoutes.mount, HealthRoutes.mount)
    val options = DeploymentOptions().setConfig(JsonObject().put("http.port", port))

    vertx.deployVerticle(HttpServerVerticle(routes), options).onComplete: ar =>
      if (ar.succeeded())
        println(s"HTTP server deployed ($ar) on port $port")
      else
        println(s"Deploy failed: ${ar.cause().getMessage}")
        vertx.close()

    // val wizardOutboundPort: WizardOutboundPort = VertxEventBusAdapter(vertx)
    // Todo: start HTTP / WebSocket server on Vert.x to expose the engine port
    // val wizardEnginePort: WizardInboundPort = WizardGameAdapter(vertx, wizardOutboundPort)
    // val wizardAIPort: WizardAIPort = WizardPrologAdapter(wizardEnginePort)
