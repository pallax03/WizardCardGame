package it.unibo.pps.wizard.application.bot

import io.vertx.core.Vertx
import it.unibo.pps.wizard.engine.ports.AIPort
import it.unibo.pps.wizard.engine.ports.GameEngineInboundPort

import scala.concurrent.Future
import scala.concurrent.Promise

/**
 * Manages the lifecycle of bot managers within the application.
 *
 * This class is responsible for deploying and undeploying bot manager verticles,
 * ensuring that only one bot manager is active at any given time.
 *
 * @param vertx        The Vert.x instance used for deploying verticles.
 * @param inboundPort  The inbound port for receiving events from the game engine.
 * @param aiPort       The AI port for handling bot decision-making.
 */
class BotLifecycleManager(
                           private val vertx: Vertx,
                           private val inboundPort: GameEngineInboundPort,
                           private val aiPort: AIPort
):
  private var currentBotDeploymentId: Option[String] = None

  def setupNewBotManager(): Future[Unit] =
    val promise = Promise[Unit]()
    val newBotVerticle = BotManagerVerticle(inboundPort, aiPort)

    vertx
      .deployVerticle(newBotVerticle)
      .onComplete: event =>
        if event.succeeded() then
          val newId = event.result()
          currentBotDeploymentId = Some(newId)
          println(s"New bot manager deployed with ID $newId.")
          promise.success(())
        else
          println(s"Failed to deploy new bot manager: ${event.cause().getMessage}")
          promise.failure(event.cause())
    promise.future

  def shutdownBotManager(): Future[Unit] =
    currentBotDeploymentId match
      case Some(deploymentId) =>
        val promise = Promise[Unit]()
        vertx
          .undeploy(deploymentId)
          .onComplete: event =>
            if event.succeeded() then
              println(s"Bot manager with deployment ID $deploymentId undeployed.")
              currentBotDeploymentId = None
              promise.success(())
            else
              println(s"Failed to undeploy bot manager: ${event.cause().getMessage}")
              promise.failure(event.cause())
        promise.future
      case None =>
        println("No bot manager is currently deployed.")
        Future.successful(())
