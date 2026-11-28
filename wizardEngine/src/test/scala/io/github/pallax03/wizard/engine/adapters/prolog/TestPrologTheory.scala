package io.github.pallax03.wizard.engine.adapters.prolog

import scala.util.Random

import io.github.pallax03.wizard.engine.model.basic.*
import io.github.pallax03.wizard.engine.model.basic.cards.{Card, Deck}
import io.github.pallax03.wizard.engine.model.basic.gameplay.{Table, Trump}
import io.github.pallax03.wizard.engine.model.rules.TableRules.*
import io.github.pallax03.wizard.util.PrologEngine

import org.scalatest.matchers.should.Matchers
import org.scalatest.prop.TableDrivenPropertyChecks
import org.scalatest.wordspec.AnyWordSpec

class TestPrologTheory extends AnyWordSpec with Matchers with TableDrivenPropertyChecks:

  import BasicTestDSL.*

  private val engine = WizardPrologEngine()

  "Prolog theory" when:
    "choosing trump color" should:
      "select the dominant color in hand" in:
        val hand = ((Two of Red) - (Five of Red) - (Ten of Blue) - wizard).asHand
        engine.chooseTrumpColor(hand) shouldBe Some(Red)

      "prefer high-rank quality over raw low-card count" in:
        val hand =
          ((Twelve of Blue) - (Thirteen of Blue) - (One of Red) - (Two of Red) - (Three of Red)).asHand
        engine.chooseTrumpColor(hand) shouldBe Some(Blue)

      "fallback to a valid color when hand has only special cards" in:
        val hand = (wizard - jester - wizard).asHand
        val chosen = engine.chooseTrumpColor(hand)
        chosen shouldBe defined
        Card.Color.values should contain(chosen.get)

    "placing and adjusting bids" should:
      "keep bid within [0, hand.size] for active trump and no-trump rounds" in:
        val cases = Table(
          ("hand", "trump"),
          (
            ((One of Red) - (Four of Red) - wizard - (Twelve of Yellow) - (Thirteen of Blue)).asHand,
            Option(One of Yellow).asTrump
          ),
          (((Thirteen of Red) - (Thirteen of Blue) - jester).asHand, Trump.Absent),
          (((Two of Green) - (Three of Blue) - jester).asHand, Option(jester).asTrump),
          (wizard.asHand, Option(One of Red).asTrump)
        )
        forAll(cases): (hand, trump) =>
          val bid = engine.placeBid(hand, trump)
          bid shouldBe defined
          bid.get should be >= 0
          bid.get should be <= hand.toList.size

      "bid >= 1 with 5 low trumps and bid >= 2 on strong no-trump hands" in:
        val lowTrumps =
          ((One of Red) - (Two of Red) - (Three of Red) - (Four of Red) - (Five of Red)).asHand
        engine.placeBid(lowTrumps, Option(Ten of Red).asTrump).get should be >= 1

        val noTrumpStrong = ((Thirteen of Red) - (Twelve of Blue) - (Eleven of Green)).asHand
        engine.placeBid(noTrumpStrong, Trump.Absent).get should be >= 2

      "adjustBid consistently with wizards and jesters" in:
        val twoWizards = (wizard - wizard - (Ten of Red)).asHand
        engine.adjustBid(twoWizards, 2) shouldBe Some(3)

        val wizardAndJester = (wizard - jester).asHand
        engine.adjustBid(wizardAndJester, 1) shouldBe Some(0)

      "never return the rejected bid and stay within [0, hand.size] in adjustBid" in:
        val hand = ((Two of Red) - jester - wizard - (Ten of Blue)).asHand
        for rejected <- 0 to hand.toList.size + 1 do
          val adjusted = engine.adjustBid(hand, rejected)
          adjusted shouldBe defined
          adjusted.get should not be rejected
          adjusted.get should be >= 0
          adjusted.get should be <= hand.toList.size

      "adjust bid higher under opponent underbid pressure and lower under overbid pressure" in:
        val marginalHand =
          ((Ten of Red) - (Nine of Red) - (Twelve of Blue) - (Eleven of Green) - (Three of Yellow) - (Four of Blue)).asHand
        val trump = Option(Two of Red).asTrump
        val underbid = engine
          .placeBid(marginalHand, trump, totalPlayers = 4, bidsPlaced = 2, otherBidsSum = 0)
          .get
        val overbid = engine
          .placeBid(marginalHand, trump, totalPlayers = 4, bidsPlaced = 2, otherBidsSum = 6)
          .get
        underbid should be >= overbid

        val threePlayers = engine.placeBid(marginalHand, trump, totalPlayers = 3).get
        val sixPlayers = engine.placeBid(marginalHand, trump, totalPlayers = 6).get
        threePlayers should be >= sixPlayers

    "selecting best playable card" should:
      "always return a legal card across 100 seeded random game states" in:
        val rng = new Random(12345L)
        for _ <- 1 to 100 do
          val handSize = 1 + rng.nextInt(20)
          val deck = Deck.create(rng).cards
          val hand = deck.take(handSize).toList.asHand
          val trump = rng.nextInt(4) match
            case 0 => Trump.Absent
            case 1 => Trump.Jester(Card.Jester(0))
            case _ =>
              Trump.Standard(Card.Standard(Card.Color.values(rng.nextInt(4)), Card.Rank.One))
          val tablePlays = rng.nextInt(4)
          val table = deck
            .slice(handSize, handSize + tablePlays)
            .zipWithIndex
            .foldLeft(
              io.github.pallax03.wizard.engine.model.basic.gameplay.Table.empty
            ):
              case (t, (c, idx)) => t + (PlayerId(idx + 1), c)
          val bid = rng.nextInt(handSize + 1)
          val tricks = rng.nextInt(handSize + 1)

          val tableCards = deck.slice(handSize, handSize + tablePlays).toList
          val remainingPlayers = rng.nextInt(4)
          val tableOverbid = rng.nextInt(7) - 3
          val chosen = engine.bestPlayableCard(
            hand = hand,
            winningCard = table.evaluateTrick(trump),
            followingColor = table.followingColor,
            trump = trump,
            playerBid = bid,
            playerTrick = tricks,
            tableCards = tableCards,
            remainingPlayers = remainingPlayers,
            tableOverbid = tableOverbid
          )
          chosen shouldBe defined
          hand.legalCards(table) should contain(chosen.get)

      "choose the tactically optimal card across key game scenarios" in:
        val puzzles = Table(
          ("scenario", "hand", "winning", "follow", "trump", "bid", "tricks", "expected"),
          (
            "lowest winning follow-suit card",
            ((Eleven of Yellow) - (Thirteen of Yellow) - wizard - (Two of Blue)).asHand,
            Some(Ten of Yellow),
            Some(Yellow),
            Option(Five of Red).asTrump,
            2,
            0,
            Eleven of Yellow
          ),
          (
            "shed wizard under wizard when wanting to lose",
            ((Two of Yellow) - wizard - (Ten of Red)).asHand,
            Some(wizard),
            Some(Yellow),
            Option(Five of Red).asTrump,
            1,
            1,
            wizard
          ),
          (
            "cheap non-trump over low trump on jester",
            ((Two of Red) - (Five of Blue)).asHand,
            Some(jester),
            None,
            Option(Ten of Red).asTrump,
            1,
            0,
            Five of Blue
          ),
          (
            "preserve jester and create void when unable to win",
            ((Two of Blue) - jester - (Twelve of Red)).asHand,
            Some(Thirteen of Yellow),
            Some(Yellow),
            Option(Five of Green).asTrump,
            1,
            0,
            Two of Blue
          ),
          (
            "play jester to protect high cards and trumps",
            ((Thirteen of Blue) - (Twelve of Green) - jester).asHand,
            Some(wizard),
            Some(Yellow),
            Option(Five of Green).asTrump,
            2,
            0,
            jester
          ),
          (
            "lead jester when wanting to lose and lowest non-trump is high",
            ((Nine of Blue) - (Eleven of Red) - jester).asHand,
            None,
            None,
            Option(Five of Green).asTrump,
            0,
            0,
            jester
          ),
          (
            "save jester when a low non-trump exists",
            ((Two of Blue) - (Eleven of Red) - jester).asHand,
            None,
            None,
            Option(Five of Green).asTrump,
            0,
            0,
            Two of Blue
          ),
          (
            "avoid dumping wizard when forced to beat jester while wanting to lose",
            ((Two of Blue) - wizard).asHand,
            Some(jester),
            None,
            Option(Five of Green).asTrump,
            0,
            0,
            Two of Blue
          ),
          (
            "lead wizard when all remaining tricks are needed",
            ((Ten of Blue) - wizard).asHand,
            None,
            None,
            Option(Five of Green).asTrump,
            2,
            0,
            wizard
          )
        )
        forAll(puzzles): (_, hand, winning, follow, trump, bid, tricks, expected) =>
          engine.bestPlayableCard(hand, winning, follow, trump, bid, tricks) shouldBe Some(expected)

      "choose a legal card when leading an empty table both when wanting to win and lose" in:
        val hand = ((Two of Blue) - (Ten of Red) - wizard).asHand
        val winLead = engine.bestPlayableCard(hand, None, None, Option(Five of Green).asTrump, 2, 0)
        val loseLead =
          engine.bestPlayableCard(hand, None, None, Option(Five of Green).asTrump, 0, 0)
        winLead shouldBe defined
        loseLead shouldBe defined
        hand.toList should contain(winLead.get)
        hand.toList should contain(loseLead.get)

      "adapt card play to seat position and table pressure" in:
        val winHand = ((Eleven of Yellow) - (Thirteen of Yellow) - wizard - (Two of Blue)).asHand
        val trump = Option(Five of Red).asTrump
        engine.bestPlayableCard(
          winHand,
          Some(Ten of Yellow),
          Some(Yellow),
          trump,
          playerBid = 2,
          playerTrick = 0,
          remainingPlayers = 0,
          tableOverbid = 1
        ) shouldBe Some(Eleven of Yellow)
        engine.bestPlayableCard(
          winHand,
          Some(Ten of Yellow),
          Some(Yellow),
          trump,
          playerBid = 2,
          playerTrick = 0,
          remainingPlayers = 2,
          tableOverbid = 1
        ) shouldBe Some(Thirteen of Yellow)

        val forcedWinHand = ((Four of Yellow) - (Thirteen of Yellow)).asHand
        engine.bestPlayableCard(
          forcedWinHand,
          Some(Three of Yellow),
          Some(Yellow),
          trump,
          playerBid = 0,
          playerTrick = 0,
          remainingPlayers = 2
        ) shouldBe Some(Four of Yellow)
        engine.bestPlayableCard(
          forcedWinHand,
          Some(Three of Yellow),
          Some(Yellow),
          trump,
          playerBid = 0,
          playerTrick = 0,
          remainingPlayers = 0
        ) shouldBe Some(Thirteen of Yellow)

        val underbidHand = ((Two of Blue) - (Eleven of Red) - jester).asHand
        engine.bestPlayableCard(
          underbidHand,
          None,
          None,
          Option(Five of Green).asTrump,
          playerBid = 0,
          playerTrick = 0,
          tableOverbid = -2
        ) shouldBe Some(jester)

        val ruffHand = ((Three of Red) - (Thirteen of Red) - wizard).asHand
        engine.bestPlayableCard(
          ruffHand,
          Some(Ten of Yellow),
          Some(Yellow),
          trump,
          playerBid = 1,
          playerTrick = 0,
          tableCards = List(Ten of Yellow),
          remainingPlayers = 1,
          tableOverbid = 0
        ) shouldBe Some(Three of Red)

    "evaluating determinism, rules, and 20-card performance" should:
      val solver = PrologEngine.buildEngine(WizardPrologEngine.defaultTheory)

      "produce exactly one solution for every public API predicate" in:
        val goals = List(
          "choose_trump([card(1,red),card(2,red),wizard], C)",
          "choose_trump([wizard,jester], C)",
          "place_bid([card(13,red),wizard,jester], blue, B)",
          "place_bid([card(13,red),wizard,jester], blue, 4, 2, 1, B)",
          "adjust_bid([card(1,red),jester,wizard], 1, B)",
          "adjust_bid([card(1,red),jester,wizard], red, 4, 2, 1, 1, B)",
          "best_playable_card([card(11,yellow),wizard], card(10,yellow), yellow, red, 2, 0, C)",
          "best_playable_card([card(11,yellow),wizard], [card(10,yellow)], card(10,yellow), yellow, red, 2, 0, 2, 1, C)",
          "best_playable_card([card(2,blue),card(10,red)], none, none, green, 0, 0, C)"
        )
        goals.foreach: goal =>
          solver(goal).take(2).count(_.isSuccess) shouldBe 1

      "enforce valid card terms in beats/3" in:
        solver("beats(none, jester, red)").headOption.exists(_.isSuccess) shouldBe false
        solver("beats(wizard, none, red)").headOption.exists(_.isSuccess) shouldBe false
        solver("beats(wizard, wizard, red)").headOption.exists(_.isSuccess) shouldBe false
        solver("beats(card(1,red), card(13,blue), red)").headOption.exists(
          _.isSuccess
        ) shouldBe true
        solver("beats(card(1,red), card(13,blue), none)").headOption.exists(
          _.isSuccess
        ) shouldBe false

      "solve 20-card hands in under 200ms without exponential blowup" in:
        val hand20 = Deck.create(new Random(77L)).cards.take(20).asHand
        val startNs = System.nanoTime()
        engine.chooseTrumpColor(hand20) shouldBe defined
        engine.placeBid(hand20, Option(One of Red).asTrump) shouldBe defined
        engine.bestPlayableCard(
          hand = hand20,
          winningCard = Some(Ten of Blue),
          followingColor = Some(Blue),
          trump = Option(One of Red).asTrump,
          playerBid = 5,
          playerTrick = 2
        ) shouldBe defined
        val elapsedMs = (System.nanoTime() - startNs) / 1e6
        elapsedMs should be < 200.0
