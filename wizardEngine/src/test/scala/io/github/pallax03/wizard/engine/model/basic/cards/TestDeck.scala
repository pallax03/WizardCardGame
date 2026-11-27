package io.github.pallax03.wizard.engine.model.basic.cards

import scala.util.Random

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class TestDeck extends AnyWordSpec with Matchers:
  import Deck.TOTAL_SIZE

  "A new randomized Deck" should:
    val d = Deck.create()
    "have a size of 60 distinct cards with canonical special card IDs" in:
      d.length shouldBe TOTAL_SIZE
      d.cards.distinct should have size TOTAL_SIZE
      d.cards.collect { case Card.Wizard(id) => id }.sorted shouldBe (0 until Deck.TOTAL_WIZARD)
      d.cards.collect { case Card.Jester(id) => id }.sorted shouldBe (0 until Deck.TOTAL_JESTER)

  "A seeded Deck" should:
    "produce identical 60-card orderings for the same seed and pop cards in order" in:
      val d1 = Deck.create(new Random(42L))
      val d2 = Deck.create(new Random(42L))
      val d3 = Deck.create(new Random(99L))
      d1.length shouldBe TOTAL_SIZE
      d1.cards shouldBe d2.cards
      d1.cards should not be d3.cards

      val (remaining, drawn) = Deck.pop(3).run(d1).value
      drawn shouldBe d1.cards.take(3)
      remaining.length shouldBe (TOTAL_SIZE - 3)
