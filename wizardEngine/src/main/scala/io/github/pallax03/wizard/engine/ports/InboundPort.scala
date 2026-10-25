package io.github.pallax03.wizard.engine.ports

import scala.concurrent.Future

import io.github.pallax03.wizard.engine.lobby.{GameConfiguration, LobbyId}
import io.github.pallax03.wizard.engine.model.basic.PlayerId
import io.github.pallax03.wizard.engine.model.core.state.PlayerGameState
import io.github.pallax03.wizard.engine.model.core.{GameAction, GameActionError}

/**
 * Primary driving port in the hexagonal architecture for the Wizard game engine.
 *
 * Exposes command and query operations invoked by driving adapters (HTTP endpoints,
 * WebSocket handlers, bot workers, AFK turn timers) to control active game sessions.
 */
trait InboundPort:

  /**
   * Retrieves the current game state scoped to the perspective of the requesting player.
   *
   * Opponent cards remain hidden within [[PlayerGameState]], ensuring information hiding invariants.
   *
   * @return future containing [[PlayerGameState]], or failing with [[io.github.pallax03.wizard.engine.model.core.GameException]] if not found or corrupted.
   */
  def getState(lobbyId: LobbyId, playerId: PlayerId): Future[PlayerGameState]

  /**
   * Initializes and starts a new game session with the given participants and configuration.
   *
   * Deals round 1 cards, determines initial trump, and broadcasts initial lifecycle events.
   */
  def startGame(lobbyId: LobbyId, players: List[PlayerId], config: GameConfiguration): Future[Unit]

  /** Resumes a paused game session, re-enabling turn timers and event dispatch. */
  def resumeGame(lobbyId: LobbyId): Future[Unit]

  /** Evicts active game state and checkpoint records for the specified lobby. */
  def deleteGame(lobbyId: LobbyId): Future[Unit]

  /**
   * Submits a player move for validation and execution by the game engine.
   *
   * @return `Right(())` if the action is applied, or `Left(GameActionError)` if rejected by domain rules.
   */
  def submitAction(lobbyId: LobbyId, action: GameAction): Future[Either[GameActionError, Unit]]

  /**
   * Forces the game engine to execute an automated fallback move on behalf of an AFK player
   * whose turn timer has expired.
   */
  def forceFallbackAction(lobbyId: LobbyId, playerId: PlayerId): Future[Unit]
