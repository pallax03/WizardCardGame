package it.unibo.pps.wizard.engine.model.basic

import cards._
import bidding.Bid

/** Represents the unique identifier of a player in the game. */
opaque type PlayerId = Int

object PlayerId:
  def apply(s: Int): PlayerId = s

  extension (p: PlayerId)
    infix def holds(cards: List[Card]): (PlayerId, Hand) = (p, Hand(cards))
    infix def holds(card: Card): (PlayerId, Hand) = (p, Hand(List(card)))
    infix def place(bid: Bid): (PlayerId, Bid) = (p, bid)

    def toInt: Int = p
