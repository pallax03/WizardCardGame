package io.github.pallax03.wizard.engine.model.basic.bidding

import io.github.pallax03.wizard.engine.model.basic.PlayerId
import io.github.pallax03.wizard.engine.model.basic.gameplay.Round

/**
 * Represents the bid (predicted number of tricks to win) placed by a player for a round.
 *
 * In Wizard, a player's bid must be between `0` and the current `round` number (inclusive).
 *
 * @see [[io.github.pallax03.wizard.engine.model.rules.BiddingRules]] for bidding constraints.
 */
type Bid = Int

object Bid:
  extension (b: Bid)
    def isValid(round: Round): Boolean = b <= round

/**
 * Represents the collection of bids placed by all players in a round.
 *
 * Implemented as an opaque type over `Map[PlayerId, Bid]`.
 */
opaque type Bids = Map[PlayerId, Bid]

object Bids:
  def empty: Bids = Map.empty

  extension (b: Bids)
    def apply(p: PlayerId): Bid = b.getOrElse(p, 0)
    infix def +(entry: (PlayerId, Bid)): Bids = b + entry
    def isComplete(totalPlayers: Int): Boolean = b.size == totalPlayers
    def total: Bid = b.values.sum
