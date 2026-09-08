package io.github.pallax03.wizard.engine.model.basic

object BasicTestDSL:
  import cards.*
  export cards.Card.*

  extension (cards: List[Card])
    infix def -(other: Card): List[Card] = cards :+ other
    def asHand: Hand = Hand(cards)

  extension (card: Card)
    infix def -(other: Card): List[Card] = List(card, other)
    def asHand: Hand = Hand(List(card))

  extension (p: PlayerId) infix def plays(card: Card): (PlayerId, Card) = (p, card)

  def handsOf(entries: (PlayerId, Hand)*): Hands = Hands(entries.toMap)

export BasicTestDSL._
