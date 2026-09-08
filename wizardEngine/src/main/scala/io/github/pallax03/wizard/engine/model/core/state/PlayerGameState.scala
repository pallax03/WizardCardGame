package io.github.pallax03.wizard.engine.model.core.state

import io.github.pallax03.wizard.engine.model.basic.PlayerId

type PlayerGameState = GameState[PlayerCoreState]

object PlayerGameState:
  /**
   * Translates a ServerGameState into a PlayerGameState.
   * This limits the state visibility to only what the specified player is allowed to see
   * (e.g., hiding other players' hands).
   *
   * @param serverGameState The complete server-side game state.
   * @param playerId The ID of the player requesting the state.
   * @return A restricted PlayerGameState tailored for the specified player.
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
