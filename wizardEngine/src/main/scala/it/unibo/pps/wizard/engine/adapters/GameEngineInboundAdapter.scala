package it.unibo.pps.wizard.engine.adapters

import io.vertx.core.Vertx
import io.vertx.core.eventbus.MessageConsumer
import it.unibo.pps.wizard.engine.events.FailureEvent.ActionFailed
import it.unibo.pps.wizard.engine.events.LifecycleEvent.GameStarted
import it.unibo.pps.wizard.engine.events._
import it.unibo.pps.wizard.engine.model.basic.Players
import it.unibo.pps.wizard.engine.model.configuration.GameConfiguration
import it.unibo.pps.wizard.engine.model.core.GameAction
import it.unibo.pps.wizard.engine.model.core.GameEngine
import it.unibo.pps.wizard.engine.model.core.GameState
import it.unibo.pps.wizard.engine.ports.GameEngineInboundPort
import it.unibo.pps.wizard.engine.ports.GameEngineOutboundPort
import it.unibo.pps.wizard.util.Id
import it.unibo.pps.wizard.util.VerticleExecutor

import scala.concurrent.Future
import scala.reflect.ClassTag

/** Represents the state of the Wizard game. */
enum WizardGameState:
  case NotConfigured
  case Running(state: GameState)

/**
 * An adapter that implements the [[GameEngineInboundPort]] interface, allowing interaction with the Wizard game engine.
 *
 * @param vertx the Vert.x instance used for event handling
 * @param outboundPort the outbound port used to publish events
 */
class GameEngineInboundAdapter(private val vertx: Vertx, private val outboundPort: GameEngineOutboundPort)
    extends GameEngineInboundPort:
  private var currentState: WizardGameState = WizardGameState.NotConfigured
  private val verticleExecutor: VerticleExecutor = VerticleExecutor(this.vertx)
  private var subscriptions: Map[String, MessageConsumer[?]] = Map.empty

  override def getState: Future[WizardGameState] =
    runOnVerticle("State Retrieval"):
      this.currentState

  override def startGame(players: Players, config: GameConfiguration): Future[Unit] =
    runOnVerticle("Game Start"):
      this.currentState match
        case WizardGameState.NotConfigured =>
          val playersAndBots: Players = Players.create(players, config.numberOfBots)
          val initialState = GameEngine.initializeGame(playersAndBots.getPlayerIds)
          this.currentState = WizardGameState.Running(initialState.state)
          this.outboundPort.publishEvent(GameStarted(playersAndBots.getPlayerIds, config.botsDifficulty))
          this.outboundPort.publishAllEvents(initialState.events)
        case _ =>

  override def submitAction(action: GameAction): Future[Unit] =
    runOnVerticle(s"Action Submission: $action"):
      this.currentState match
        case WizardGameState.Running(oldState) =>
          GameEngine.processAction(oldState, action) match
            case Left(error) =>
              println(s"Error processing action: $error")
              this.outboundPort.publishEvent(ActionFailed(action.playerId, error))
            case Right(newState) =>
              newState.state match
                case _: GameState.Ended =>
                  this.currentState = WizardGameState.NotConfigured
                  this.outboundPort.publishAllEvents(newState.events)
                case _ =>
                  this.currentState = WizardGameState.Running(newState.state)
                  this.outboundPort.publishAllEvents(newState.events)
        case _ =>

  override def subscribe[T <: Event: ClassTag](handler: T => Unit): Future[String] =
    val subscriptionId: String = Id()
    runOnVerticle(s"Subscription to ${addressOf[T]} {#$subscriptionId}"):
      this.subscriptions +=
        subscriptionId ->
          this.vertx
            .eventBus()
            .consumer[T](addressOf[T], message => handler(message.body))
      subscriptionId

  override def unsubscribe(subscriptionIds: String*): Future[Unit] =
    runOnVerticle(s"Unsubscription from ${subscriptionIds.mkString(", ")}"):
      subscriptionIds.foreach: subscriptionId =>
        this.subscriptions
          .get(subscriptionId)
          .foreach: consumer =>
            consumer.unregister()
            this.subscriptions -= subscriptionId

  /**
   * Runs a given activity on the Vert.x event loop, ensuring thread safety and proper execution context.
   *
   * @param activityName A descriptive name for the activity being executed.
   * @param activity The code block representing the activity to be executed.
   * @tparam T The return type of the activity.
   * @return A Future containing the result of the activity.
   */
  private def runOnVerticle[T](activityName: String)(activity: => T): Future[T] =
    this.verticleExecutor.runLater:
      println(s"Running activity '$activityName' on verticle...")
      activity
