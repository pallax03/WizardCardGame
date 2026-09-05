package io.github.pallax03.wizard.engine.model.rules

import scala.language.postfixOps

import io.github.pallax03.wizard.engine.model.basic.*
import io.github.pallax03.wizard.engine.model.core.CardNotAllowedReasons.*
import io.github.pallax03.wizard.engine.model.core.GameError

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class TestTableRules extends AnyWordSpec with Matchers:

  import BasicTestDSL._
  import cards.Card
  import cards.Card._
  import gameplay.{Table, Trump}
  "TableRules Validation" when:
    val p1 = PlayerId(1)
    val myWizard = wizard
    "a player tries to play a card they don't hold" should:
      "return a CardNotInHand reason" in:
        val hand = (Five of Blue).asHand
        val result = (Ten of Red).validateAgainst(Table.empty, hand)
        result shouldBe Left(GameError.CardNotAllowed(CardNotInHand(hand.legalCards(Table.empty))))

    "evaluating standard rules" should:
      val c1: Card = Five of Blue
      val c2: Card = Ten of Red
      val hand = (c1 - c2 - myWizard).asHand
      "allow playing anything if there is no cards" in:
        c1.validateAgainst(Table.empty, hand) shouldBe Right(())

      "player HAS to follow the following color" in:
        val table = Table.empty + (p1 plays (Four of Blue))
        val result = c2.validateAgainst(table, hand)
        result shouldBe Left(
          GameError.CardNotAllowed(MustFollowColor(Blue, hand.legalCards(table)))
        )

      "player LACKS the following color" in:
        val table = Table.empty + (p1 plays (Four of Yellow))
        c2.validateAgainst(table, hand) shouldBe Right(())

      "always allow special cards even if player has the following color" in:
        val table = Table.empty + (p1 plays (Four of Blue))
        myWizard.validateAgainst(table, hand) shouldBe Right(())

      "always allow any standard card if table have a wizard" in:
        val table = Table.empty + (p1 plays (Four of Blue)) + (p1 plays myWizard)
        c2.validateAgainst(table, hand) shouldBe Right(())

  "TableRules Winner Evaluation" should:
    val p1 = PlayerId(1)
    val p2 = PlayerId(2)
    val p3 = PlayerId(3)
    "award the trick to the first Wizard played" in:
      val winningTrick = p2 plays wizard
      val table = Table.empty + (p1 plays (Ten of Red)) + winningTrick + (p3 plays wizard)
      val winner = table.evaluateTrick(Trump.Absent).map(c => (table.playerOf(c), c))
      winner shouldBe Some(winningTrick)

    "award the trick to the highest trump (no Wizard)" in:
      val winningTrick = p3 plays (Five of Red)
      val table = Table.empty + (p1 plays (Ten of Blue)) + (p2 plays (Two of Red)) + winningTrick
      val winner =
        table.evaluateTrick(Trump(One of Red)).map(c => (table.playerOf(c), c))
      winner shouldBe Some(winningTrick)

    "award the trick to the highest following card (no Trump and no Wizard)" in:
      val winningTrick = p2 plays (Ten of Blue)
      val table =
        Table.empty + (p1 plays (Five of Blue)) + (p2 plays (Ten of Blue)) + (p3 plays (Two of Yellow))
      val trump = Trump(One of Green)
      val winner = table.evaluateTrick(trump).map(c => (table.playerOf(c), c))
      winner shouldBe Some(winningTrick)

    "award the trick to the first played Jester if ONLY Jesters are on table" in:
      val winningTrick = p1 plays jester
      val table = Table.empty + winningTrick + (p2 plays jester) + (p3 plays jester)
      val winner = table.evaluateTrick(Trump.Absent).map(c => (table.playerOf(c), c))
      winner shouldBe Some(winningTrick)
