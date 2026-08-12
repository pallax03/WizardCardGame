package it.unibo.pps.wizard.engine.adapters.inmemory

import io.vertx.core.Vertx
import it.unibo.pps.wizard.engine.configuration.*
import it.unibo.pps.wizard.engine.model.basic.*
import it.unibo.pps.wizard.engine.model.core.*
import it.unibo.pps.wizard.engine.model.events.*
import it.unibo.pps.wizard.engine.ports.{GameEngineInboundPort, GameEngineOutboundPort}
import it.unibo.pps.wizard.util.VerticleExecutor

import scala.concurrent.Future

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
class LocalGameInboundAdapter(
    private val vertx: Vertx,
    private val outboundPort: GameEngineOutboundPort
) extends GameEngineInboundPort:
  private var currentState: WizardGameState = WizardGameState.NotConfigured
  private val verticleExecutor: VerticleExecutor = VerticleExecutor(this.vertx)

  override def getState: Future[WizardGameState] =
    runOnVerticle("State Retrieval"):
      this.currentState

  override def startGame(players: List[PlayerId], config: GameConfiguration): Future[Unit] =
    runOnVerticle("Game Start"):
      this.currentState match
        case WizardGameState.NotConfigured =>
          val playersAndBots = config.players.map(_.id)
          val initialState = GameEngine.initializeGame(playersAndBots)
          this.currentState = WizardGameState.Running(initialState.state)
          this.outboundPort.publishEvent(LifecycleEvent.GameStarted(playersAndBots))
          this.outboundPort.publishAllEvents(initialState.events)
        case _ =>

  override def submitAction(action: GameAction): Future[Unit] =
    runOnVerticle(s"Action Submission: $action"):
      this.currentState match
        case WizardGameState.Running(oldState) =>
          GameEngine.processAction(oldState, action) match
            case Left(error) =>
              println(s"Error processing action: $error")
              this.outboundPort.publishEvent(FailureEvent.ActionFailed(action.playerId, error))
            case Right(newState) =>
              newState.state match
                case _: GameState.Ended =>
                  this.currentState = WizardGameState.NotConfigured
                  this.outboundPort.publishAllEvents(newState.events)
                case _ =>
                  this.currentState = WizardGameState.Running(newState.state)
                  this.outboundPort.publishAllEvents(newState.events)
        case _ =>

  /**
   * Runs a given activity on the Vert.x event loop, ensuring thread safety and proper execution context.
   *
   * @param activityName A descriptive name for the activity being executed.
   * @param activity The code block representing the activity to be executed.
   * @tparam T The return type of the activity.
   * @return A Future containing the result of the activity.
   */
  // todo: will be removed
  private def runOnVerticle[T](activityName: String)(activity: => T): Future[T] =
    this.verticleExecutor.runLater:
      println(s"Running activity '$activityName' on verticle...")
      activity
