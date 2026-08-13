package it.unibo.pps.wizard.engine.model.core

import it.unibo.pps.wizard.engine.model.basic._
import it.unibo.pps.wizard.engine.model.basic.bidding.Bids
import it.unibo.pps.wizard.engine.model.basic.bidding.Tricks
import it.unibo.pps.wizard.engine.model.basic.gameplay.Table
/**
 * Represents the various phases and states of the Wizard card game.
 *
 * Each state encapsulates the necessary data to represent the game at a specific point in time.
 * This allows the [[GameEngine]] to perform transitions in an immutable and functional way.
 */
sealed trait GameState

object GameState:

  /** The [[core.dealerId]] is choosing the trump color after a Wizard card is revealed. */
  case class ChoosingTrump(
      core: CoreState
  ) extends GameState

  /** Players are currently placing their bids for the round. */
  case class Bidding(core: CoreState, bids: Bids, playerTurn: PlayerId) extends GameState

  /**
   * Cards are being played on the table.
   * This is the main phase where tricks are resolved and winners are determined.
   */
  case class Playing(
                      core: CoreState,
                      bids: Bids,
                      table: Table,
                      playerTurn: PlayerId,
                      tricksWon: Tricks
  ) extends GameState

  /** The final state of the game, containing the definitive scoreboard. */
  case class Ended(playersIds: List[PlayerId], scoreboard: Scoreboard) extends GameState
