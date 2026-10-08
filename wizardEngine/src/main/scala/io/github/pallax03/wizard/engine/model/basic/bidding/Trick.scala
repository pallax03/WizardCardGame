package io.github.pallax03.wizard.engine.model.basic.bidding

import io.github.pallax03.wizard.engine.model.basic.PlayerId

/** Represents the count of tricks (prese) won by a player during a round. */
type Trick = Int

/**
 * Tracks the count of tricks won by each player in the active round.
 *
 * Implemented as an opaque type over `Map[PlayerId, Trick]`.
 */
opaque type Tricks = Map[PlayerId, Trick]

object Tricks:
  def empty: Tricks = Map.empty

  extension (t: Tricks)
    def apply(p: PlayerId): Trick = t.getOrElse(p, 0)
    infix def addTrickTo(p: PlayerId): Tricks = t.updated(p, t.getOrElse(p, 0) + 1)
