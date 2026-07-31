package it.unibo.pps.wizard.engine.model.basic.bidding

import it.unibo.pps.wizard.engine.model.basic.PlayerId
import it.unibo.pps.wizard.engine.model.basic.gameplay.Round

/** Represents the bid (predicted number of tricks to win) placed by a player for a round. */
type Bid = Int

object Bid:
  def apply(value: Int): Bid = value

  extension (b: Bid)
    /**
     * Checks if the bid is valid for the given round.
     * A bid is valid if it does not exceed the total number of cards dealt in that round.
     *
     * @param round the current game round.
     * @return true if the bid is less than or equal to the round number, false otherwise.
     */
    def isValid(round: Round): Boolean = b <= round.value

/** Represents the collection of bids placed by all players in a round. */
opaque type Bids = Map[PlayerId, Bid]

object Bids:
  def empty: Bids = Map.empty

  extension (b: Bids)
    /**
     * Returns the bid placed by a specific player, defaulting to 0 if not found.
     *
     * @param p the player ID.
     * @return the player's bid.
     */
    def apply(p: PlayerId): Bid = b.getOrElse(p, 0)

    /**
     * Adds or updates a player's bid in the collection.
     *
     * @param entry a tuple associating a player ID with their bid.
     * @return the updated [[Bids]] collection.
     */
    infix def +(entry: (PlayerId, Bid)): Bids = b + entry

    /**
     * Checks if all players have placed their bids.
     *
     * @param totalPlayers the total number of players in the game.
     * @return true if the number of recorded bids matches the player count.
     */
    def isComplete(totalPlayers: Int): Boolean = b.size == totalPlayers

    /**
     * Calculates the sum of all bids placed in the current round.
     *
     * @return the total number of bids.
     */
    def total: Bid = b.values.sum
