package it.unibo.pps.wizard

object Main:

  def main(args: Array[String]): Unit =
    try
      println("Starting wizard system...")
      val port = sys.env.getOrElse("PORT", "8080").toInt
      // val vertx = Vertx.vertx()
      // val wizardOutboundPort: WizardOutboundPort = VertxEventBusAdapter(vertx)

      println(s"Wizard system initialized on port $port. Awaiting connections...")

      // Todo: start HTTP / WebSocket server on Vert.x to expose the engine port
      // val wizardEnginePort: WizardInboundPort = WizardGameAdapter(vertx, wizardOutboundPort)
      // val wizardAIPort: WizardAIPort = WizardPrologAdapter(wizardEnginePort)

    catch
      case error: Throwable =>
        println("Error during wizard system startup: " + error.getMessage)
        error.printStackTrace()
        System.exit(1)
