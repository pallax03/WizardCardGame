package it.unibo.pps.wizard.engine.model.basic.cards

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import scala.language.postfixOps

class TestCard extends AnyWordSpec with Matchers:
  import Card.*
  "A card" should:
    "Create a Standard" in:
      Thirteen of Red shouldBe a[Standard]

    "Create a Wizard" in:
      wizard shouldBe a[Wizard]

    "Create a Jester" in:
      jester shouldBe a[Jester]

    "guarantee that special cards are unique instances" in:
      wizard should not equal wizard

  "Some Cards" should:
    "create a chain of Cards" in:
      val myCards: List[Card] =
        (Five of Red) - (Four of Yellow) - wizard - (Ten of Green) - jester - (Thirteen of Blue)
      myCards should have size 6
      myCards.head shouldBe (Five of Red)
      myCards(1) shouldBe (Four of Yellow)
      myCards(2) shouldBe a[Wizard]
      myCards(3) shouldBe (Ten of Green)
      myCards(4) shouldBe a[Jester]
      myCards(5) shouldBe (Thirteen of Blue)
