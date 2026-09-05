package io.github.pallax03.wizard.engine.ports

import scala.concurrent.Future

import io.github.pallax03.wizard.engine.configuration.GameConfiguration
import io.github.pallax03.wizard.engine.lobby.LobbyId
import io.github.pallax03.wizard.engine.model.basic.PlayerId
import io.github.pallax03.wizard.engine.model.core.state.PlayerGameState
import io.github.pallax03.wizard.engine.model.core.{GameAction, GameError}

/**
 * Inbound port for the Wizard game engine.
 * This trait defines the methods that can be called by external components to interact with the game engine.
 */
trait InboundPort:

  /**
   * Retrieves the current state of the game.
   *
   * @return a Future containing the current GameState for the given player
   * @throws GameException if the state cannot be retrieved because it's corrupted or inconsistent.
   */
  def getState(lobbyId: LobbyId, playerId: PlayerId): Future[PlayerGameState]

  /**
   * Starts a new game with the specified players and configuration.
   *
   * @param lobbyId the identifier of the lobby
   * @param players the players participating in the game
   * @param config the configuration for the game
   * @return a Future indicating the completion of the game start process
   * @throws GameException if the initialization logic fails due to an inconsistent state.
   */
  def startGame(lobbyId: LobbyId, players: List[PlayerId], config: GameConfiguration): Future[Unit]

  /**
   * Resumes a paused game for the specified lobby.
   *
   * @param lobbyId the identifier of the lobby
   * @return a Future indicating the completion of the game resume process
   */
  def resumeGame(lobbyId: LobbyId): Future[Unit]

  /**
   * Submits a game action for processing.
   *
   * @param lobbyId the identifier of the lobby
   * @param action the game action to submit
   * @return a Future indicating the completion of the action submission, or the domain GameError if invalid
   * @throws GameException if processing the action encounters a critical system error (corrupted state).
   */
  def submitAction(lobbyId: LobbyId, action: GameAction): Future[Either[GameError, Unit]]
