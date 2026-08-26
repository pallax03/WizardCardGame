package it.unibo.pps.wizard.engine.model.core.state

import it.unibo.pps.wizard.engine.model.basic._
import it.unibo.pps.wizard.engine.model.basic.bidding.Bids
import it.unibo.pps.wizard.engine.model.basic.bidding.Tricks
import it.unibo.pps.wizard.engine.model.basic.gameplay.Table


/** Represents the various phases and states of the Wizard card game. */
sealed trait GameState[+C <: CoreState]

object GameState:

  case class ChoosingTrump[C <: CoreState](
      core: C
  ) extends GameState[C]

  case class Bidding[C <: CoreState](core: C, bids: Bids, playerTurn: PlayerId) extends GameState[C]

  case class Playing[C <: CoreState](
      core: C,
      bids: Bids,
      table: Table,
      playerTurn: PlayerId,
      tricksWon: Tricks
  ) extends GameState[C]

  case class Ended(playersIds: List[PlayerId], scoreboard: Scoreboard) extends GameState[Nothing]

type ServerGameState = GameState[ServerCoreState]
type PlayerGameState = GameState[PlayerCoreState]

object PlayerGameState:
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
