package it.unibo.pps.wizard.engine.model.basic

object BasicTestDSL:
  import cards.*

  extension (cards: List[Card]) def asHand: Hand = Hand(cards)
  extension (card: Card) def asHand: Hand = Hand(List(card))
  extension (p: PlayerId) infix def plays(card: Card): (PlayerId, Card) = (p, card)

  def handsOf(entries: (PlayerId, Hand)*): Hands = Hands(entries.toMap)
