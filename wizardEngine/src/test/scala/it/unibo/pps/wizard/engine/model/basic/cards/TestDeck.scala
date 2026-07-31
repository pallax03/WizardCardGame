package it.unibo.pps.wizard.engine.model.basic.cards

import it.unibo.pps.wizard.engine.model.basic.cards.Card
import it.unibo.pps.wizard.engine.model.basic.cards.Deck
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class TestDeck extends AnyWordSpec with Matchers:
  import Deck.TOTAL_SIZE

  "A new randomized Deck" should:
    val d = Deck.create
    "have a size of 60 cards" in:
      d.length shouldBe TOTAL_SIZE

  "A custom Deck" should:
    import Card.*
    val c1: Card = One of Red
    val c2: Card = One of Blue
    val c3: Card = Thirteen of Green
    val c4: Card = One of Blue
    val d = Deck.create(c1 - c2 - c3 - c4)
    val nCards: Int = 3

    "have a size of 3 cards, checking for duplicates" in:
      d.length shouldBe nCards

    "pop 3 cards in the same order" in:
      val drawnAction = Deck.pop(nCards + 1)
      val (remainingDeck, drawnCards) = drawnAction.run(d).value
      drawnCards.length shouldBe nCards
      remainingDeck.length shouldBe 0
      drawnCards shouldEqual (c1 - c2 - c3)
