package io.github.pallax03.wizard.engine.model.basic.gameplay

import scala.language.postfixOps

import io.github.pallax03.wizard.engine.model.basic._

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class TestTable extends AnyWordSpec with Matchers:

  import BasicTestDSL.*
  import cards.Card
  import cards.Card.*

  "A Table" when:
    val p1 = PlayerId(1)
    val p2 = PlayerId(2)
    val p3 = PlayerId(3)
    "empty" should:
      val table = Table.empty
      "be empty and have no leader" in:
        table.playedCards shouldBe empty
        table.followingColor shouldBe None

    "receiving plays" should:
      val cardP1: Card = Ten of Red
      val cardP2: Card = wizard
      val table = Table.empty
        + (p1 plays cardP1)
        + (p2 plays cardP2)
      "store the plays in chronological order" in:
        table.playedCards should have size 2
        table.playedCards shouldEqual (cardP1 - cardP2)

      "identify the player of a specific card" in:
        table.playerOf(cardP1) shouldBe Some(p1)
        table.playerOf(cardP2) shouldBe Some(p2)
        table.playerOf(Five of Blue) shouldBe None

    "evaluating the leader card (suit to follow)" should:
      "set the first standard card as leader" in:
        val t = Table.empty + (p1 plays (Four of Blue)) + (p2 plays (Ten of Red))
        t.followingColor shouldBe Some(Blue)

      "ignore leading Jesters and take the next standard card" in:
        val t =
          Table.empty + (p1 plays jester) + (p2 plays (Eight of Green)) + (p3 plays (Two of Green))
        t.followingColor shouldBe Some(Green)

      "have NO leader there is a Wizard" in:
        val t = Table.empty + (p1 plays jester) + (p3 plays (Ten of Yellow)) + (p2 plays wizard)
        t.followingColor shouldBe None

      "have NO leader if only Jesters are played" in:
        val t = Table.empty + (p1 plays jester) + (p2 plays jester)
        t.followingColor shouldBe None
