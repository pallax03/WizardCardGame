package io.github.pallax03.wizard.engine.model.basic.cards

import io.github.pallax03.wizard.engine.model.basic._
import io.github.pallax03.wizard.engine.model.core.GameException
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import scala.language.postfixOps

class TestHand extends AnyWordSpec with Matchers:
  import BasicTestDSL._
  import cards._
  import cards.Card.*
  import cards.Hand.*

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
    val c2 = jester

    "be correctly created empty" in:
      val emptyHands = Hands.empty
      emptyHands.areEmpty shouldBe true

    "throw GameException if the player is not found" in:
      val emptyHands = Hands.empty
      assertThrows[GameException] {
        emptyHands.getHand(p1)
      }

    "retrieve a player's hand if present" in:
      val h = Hand(List(c1, c2))
      val hands = Hands(Map(p1 -> h))

      val p1Hand = hands.getHand(p1)
      p1Hand.toList should have size 2
      p1Hand.contains(c1) shouldBe true

      assertThrows[GameException] {
        hands.getHand(p3)
      }

    "remove a specific card from a player's hand" in:
      val h = Hand(List(c1, c2))
      val hands = Hands(Map(p1 -> h))

      val updatedHands = hands.remove(p1, c1)
      val newP1Hand = updatedHands.getHand(p1)

      newP1Hand.toList should have size 1
      newP1Hand.contains(c1) shouldBe false
      newP1Hand.contains(c2) shouldBe true

    "throw GameException if the player is not found during removal" in:
      val hands = Hands(Map(p1 -> Hand(List(c1))))
      assertThrows[GameException] {
        hands.remove(p3, c1)
      }

    "evaluate areEmpty correctly based on inner hands" in:
      val reallyEmpty = handsOf(
        p1 holds List.empty[Card],
        p2 holds List.empty[Card]
      )
      reallyEmpty.areEmpty shouldBe true

      val partiallyEmpty = handsOf(p1 holds c1, p2 holds List.empty[Card])
      partiallyEmpty.areEmpty shouldBe false
