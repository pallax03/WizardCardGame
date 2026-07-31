package it.unibo.pps.wizard.engine.model.basic.cards

import it.unibo.pps.wizard.engine.model.basic.BasicTestDSL._
import it.unibo.pps.wizard.engine.model.basic.PlayerId
import it.unibo.pps.wizard.engine.model.basic.cards.Card.jester
import it.unibo.pps.wizard.engine.model.basic.cards.Card.wizard
import it.unibo.pps.wizard.engine.model.basic.cards._
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import scala.language.postfixOps

class TestHand extends AnyWordSpec with Matchers:
  import Card.*
  import Hand.*

  "A Hand" when:
    "created empty" should:
      val emptyHand = Hand.empty

      "have size 0 and be completely empty" in:
        emptyHand.isEmpty shouldBe true
        emptyHand.toList shouldBe empty
        emptyHand.contains(jester) shouldBe false

    "interacting with cards" should:
      val c1 = Five of Blue
      val c2 = jester
      val notInHand = wizard
      val hand = Hand(c1 - c2)

      "return the correct size and emptiness state" in:
        hand.toList should have size 2
        hand.isEmpty shouldBe false

      "check containment correctly" in:
        hand.contains(c1) shouldBe true
        hand.contains(c2) shouldBe true
        hand.contains(notInHand) shouldBe false

      "allow removing an existing card" in:
        val smallerHand = Hand.without(hand, c1)
        smallerHand.toList should have size 1
        smallerHand.contains(c1) shouldBe false
        smallerHand.contains(c2) shouldBe true

      "remain unchanged when removing a card not present" in:
        val sameHand = Hand.without(hand, notInHand)
        sameHand.toList should have size 2
        sameHand.toList should contain theSameElementsInOrderAs List(c1, c2)

      "convert to List properly" in:
        hand.toList shouldBe List(c1, c2)

  "A Hands" should:
    val p1 = PlayerId(1)
    val p2 = PlayerId(2)
    val p3 = PlayerId(3)
    val c1 = Five of Red
    val p2Wizard: Card = wizard

    "be correctly created empty" in:
      val emptyHands = Hands.empty
      emptyHands.areEmpty shouldBe true
      emptyHands.getHand(p1) shouldBe None

    "be correctly queried for players" in:
      val hands = handsOf(
        p1 holds (c1 - jester),
        p2 holds p2Wizard
      )

      hands.areEmpty shouldBe false

      val p1Hand = hands.getHand(p1)
      p1Hand shouldBe defined
      p1Hand.get.toList should have size 2

      hands.getHand(p3) shouldBe None

    "remove cards from a player's hand successfully" in:
      val hands = handsOf(p1 holds (c1 - jester))

      val updatedHandsOpt = hands.remove(p1, c1)

      updatedHandsOpt shouldBe defined
      val newP1Hand = updatedHandsOpt.get.getHand(p1).get
      newP1Hand.toList should have size 1
      newP1Hand.contains(c1) shouldBe false

    "return None when trying to remove a card from a non-existent player" in:
      val hands = handsOf(p1 holds c1)
      hands.remove(p3, c1) shouldBe None

    "evaluate areEmpty correctly based on inner hands" in:
      val reallyEmpty = handsOf(
        p1 holds List.empty[Card],
        p2 holds List.empty[Card]
      )
      reallyEmpty.areEmpty shouldBe true

      val partiallyEmpty = handsOf(p1 holds c1, p2 holds List.empty[Card])
      partiallyEmpty.areEmpty shouldBe false
