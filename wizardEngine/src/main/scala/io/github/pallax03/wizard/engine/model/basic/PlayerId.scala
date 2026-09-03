package io.github.pallax03.wizard.engine.model.basic

import io.github.pallax03.wizard.engine.model.basic.bidding.Bid
import io.github.pallax03.wizard.engine.model.basic.cards.*

/** Represents the unique identifier of a player in the game. */
opaque type PlayerId = Int

object PlayerId:
  def apply(s: Int): PlayerId = s

  extension (p: PlayerId)
    infix def holds(cards: List[Card]): (PlayerId, Hand) = (p, Hand(cards))
    infix def holds(card: Card): (PlayerId, Hand) = (p, Hand(List(card)))
    infix def place(bid: Bid): (PlayerId, Bid) = (p, bid)

    def toInt: Int = p
