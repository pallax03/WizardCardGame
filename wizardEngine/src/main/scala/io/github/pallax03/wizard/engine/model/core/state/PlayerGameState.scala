package io.github.pallax03.wizard.engine.model.core.state

import io.github.pallax03.wizard.engine.model.basic.PlayerId

/**
 * Client-facing game state projection wrapping a [[PlayerCoreState]].
 *
 * Guarantees that only the targeted player's hand is visible, preserving match privacy.
 */
type PlayerGameState = GameState[PlayerCoreState]

object PlayerGameState:
  /**
   * Projects a complete [[ServerGameState]] into a sanitized [[PlayerGameState]] for the given player.
   *
   * Strips out other players' private hands while retaining all publicly visible game state
   * (bids, table, tricks won, scoreboard, and trump).
   *
   * @param serverGameState the complete authoritative server-side match state.
   * @param playerId        the ID of the player requesting the state projection.
   * @return a [[PlayerGameState]] containing only the cards visible to that player.
   * @throws GameException.CorruptedHand if the player's hand is missing from the server state.
   */
  def from(
      serverGameState: ServerGameState,
      playerId: PlayerId
  ): PlayerGameState =
    serverGameState match
      case GameState.ChoosingTrump(core) =>
        GameState.ChoosingTrump(PlayerCoreState.from(core, playerId))
      case GameState.Bidding(core, bids, turn) =>
        GameState.Bidding(PlayerCoreState.from(core, playerId), bids, turn)
      case GameState.Playing(core, bids, table, turn, tricks) =>
        GameState.Playing(PlayerCoreState.from(core, playerId), bids, table, turn, tricks)
      case GameState.Ended(playersIds, scoreboard) =>
        GameState.Ended(playersIds, scoreboard)
