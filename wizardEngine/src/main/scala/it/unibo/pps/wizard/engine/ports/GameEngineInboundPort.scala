package it.unibo.pps.wizard.engine.ports

import it.unibo.pps.wizard.engine.adapters.WizardGameState
import it.unibo.pps.wizard.engine.configuration.GameConfiguration
import it.unibo.pps.wizard.engine.model.basic.PlayerId
import it.unibo.pps.wizard.engine.model.core.GameAction

import scala.concurrent.Future

/**
 * Inbound port for the Wizard game engine.
 * This trait defines the methods that can be called by external components to interact with the game engine.
 */
trait GameEngineInboundPort:

  /**
   * Retrieves the current state of the game.
   *
   * @return a Future containing the current WizardGameState
   */
  def getState: Future[WizardGameState]

  /**
   * Starts a new game with the specified players and configuration.
   *
   * @param players the players participating in the game
   * @param config the configuration for the game
   * @return a Future indicating the completion of the game start process
   */
  def startGame(players: List[PlayerId], config: GameConfiguration): Future[Unit]

  /**
   * Submits a game action for processing.
   *
   * @param action the game action to submit
   * @return a Future indicating the completion of the action submission
   */
  def submitAction(action: GameAction): Future[Unit]