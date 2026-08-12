package it.unibo.pps.wizard.engine.adapters

import io.vertx.core.Vertx
import it.unibo.pps.wizard.engine.configuration.*
import it.unibo.pps.wizard.engine.lobby.LobbyId
import it.unibo.pps.wizard.engine.model.basic.*
import it.unibo.pps.wizard.engine.model.core.*
import it.unibo.pps.wizard.engine.model.events.*
import it.unibo.pps.wizard.engine.ports.{GameEngineInboundPort, GameEngineOutboundPort}
import it.unibo.pps.wizard.util.VerticleExecutor

import scala.concurrent.Future

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
  private val activeGames = scala.collection.concurrent.TrieMap[LobbyId, WizardGameState]()
  private val verticleExecutor: VerticleExecutor = VerticleExecutor(this.vertx)

  /** @inheritdoc */
  override def getState(lobbyId: LobbyId, playerId: PlayerId): Future[GameState] =
    runOnVerticle(s"State Retrieval for $lobbyId"):
      this.activeGames.get(lobbyId) match
        case Some(WizardGameState.Running(state)) => state
        case _ => throw new IllegalStateException("Game not running")

  /** @inheritdoc */
  override def startGame(lobbyId: LobbyId, players: List[PlayerId], config: GameConfiguration): Future[Unit] =
    runOnVerticle(s"Game Start for $lobbyId"):
      this.activeGames.get(lobbyId) match
        case Some(WizardGameState.Running(_)) =>
          // Already running
        case _ =>
          val playersAndBots = config.players.map(_.id)
          val initialState = GameEngine.initializeGame(playersAndBots)
          this.activeGames.put(lobbyId, WizardGameState.Running(initialState.state))
          this.outboundPort.publish(lobbyId, LifecycleEvent.GameStarted(playersAndBots))
          this.outboundPort.publish(lobbyId, initialState.events*)

  /** @inheritdoc */
  override def submitAction(lobbyId: LobbyId, action: GameAction): Future[Unit] =
    runOnVerticle(s"Action Submission for $lobbyId: $action"):
      this.activeGames.get(lobbyId) match
        case Some(WizardGameState.Running(oldState)) =>
          GameEngine.processAction(oldState, action) match
            case Left(error) =>
              println(s"Error processing action in $lobbyId: $error")
              this.outboundPort.publish(lobbyId, FailureEvent.ActionFailed(action.playerId, error))
            case Right(newState) =>
              newState.state match
                case _: GameState.Ended =>
                  this.activeGames.remove(lobbyId)
                  this.outboundPort.publish(lobbyId, newState.events*)
                case _ =>
                  this.activeGames.put(lobbyId, WizardGameState.Running(newState.state))
                  this.outboundPort.publish(lobbyId, newState.events*)
        case _ =>
          // Game not found or not running

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
