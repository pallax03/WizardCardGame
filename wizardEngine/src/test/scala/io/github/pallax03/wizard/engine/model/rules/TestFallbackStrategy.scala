package io.github.pallax03.wizard.engine.model.rules

import scala.util.Random

import io.github.pallax03.wizard.engine.*
import io.github.pallax03.wizard.engine.model.basic.*
import io.github.pallax03.wizard.engine.model.basic.bidding.{Bids, Tricks}
import io.github.pallax03.wizard.engine.model.basic.gameplay.{Round, Table}
import io.github.pallax03.wizard.engine.model.core.GameAction
import io.github.pallax03.wizard.engine.model.core.state.{
  GameState,
  PlayerCoreState,
  ServerCoreState
}

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class TestFallbackStrategy extends AnyWordSpec with Matchers:
  import cards.Card

  "FallbackStrategy" when:
    val p1 = PlayerId(1)
    val players = List(p1, PlayerId(2), PlayerId(3))

    def coreWith(hand: List[Card], round: Round, trump: Option[Card] = None): PlayerCoreState =
      PlayerCoreState.from(
        ServerCoreState
          .initialize(players, round)
          .copy(hands = handsOf(p1 holds hand), trump = trump.asTrump),
        p1
      )

    "handling ChoosingTrump" should:
      "pick the most frequent suit in hand for the dealer" in:
        given Random = new Random(42L)
        val hand = (Two of Red) - (Five of Red) - (Nine of Red) - (Ten of Blue)
        val state = GameState.ChoosingTrump(coreWith(hand, 4))
        FallbackStrategy.fallbackMove(state, p1) shouldBe Some(
          GameAction.ResolveTrumpColor(p1, Red)
        )

      "return None if not the dealer" in:
        val state = GameState.ChoosingTrump(coreWith(List(Two of Red), 1))
        FallbackStrategy.fallbackMove(state, PlayerId(2)) shouldBe None

    "handling Bidding" should:
      "bid higher on strong hands, respect Hook Rule, and stay within 0..round" in:
        val strongHand = List(wizard(0), wizard(1), Thirteen of Red, Twelve of Red)
        val weakHand = List(jester(0), jester(1), Two of Blue, Three of Green)
        val round = 4
        given Random = new Random(7L)

        val strongBids = (1 to 20).map: _ =>
          val state = GameState.Bidding(
            coreWith(strongHand, round, Some(Five of Red)),
            Bids.empty + (PlayerId(2) -> 0) + (PlayerId(3) -> 0),
            p1
          )
          FallbackStrategy.fallbackMove(state, p1) match
            case Some(GameAction.PlaceBid(_, b)) =>
              b should be >= 0
              b should be <= 4
              b should not be 4
              b
            case other => fail(s"Unexpected: $other")

        val weakBids = (1 to 20).map: _ =>
          val state = GameState.Bidding(
            coreWith(weakHand, round, Some(Five of Red)),
            Bids.empty + (PlayerId(2) -> 2) + (PlayerId(3) -> 2),
            p1
          )
          FallbackStrategy.fallbackMove(state, p1) match
            case Some(GameAction.PlaceBid(_, b)) =>
              b should be >= 0
              b should be <= 4
              b should not be 0
              b
            case other => fail(s"Unexpected: $other")

        strongBids.sum.toDouble / strongBids.size should be > (weakBids.sum.toDouble / weakBids.size)

      "return None when not player's turn" in:
        val state = GameState.Bidding(coreWith(List(Two of Red), 1), Bids.empty, PlayerId(2))
        FallbackStrategy.fallbackMove(state, p1) shouldBe None

    "handling Playing" should:
      "prefer winning cards on non-empty table when wanting tricks and losing cards when avoiding" in:
        val cards = List(jester(0), Five of Blue, Thirteen of Blue)
        val core = coreWith(cards, 3, Some(Two of Red))
        val table = Table.empty + (PlayerId(2) -> (Ten of Blue))

        val wantWinPlays = (1 to 40).map: i =>
          given Random = new Random(i.toLong)
          val state = GameState.Playing(core, Bids.empty + (p1 -> 2), table, p1, Tricks.empty)
          FallbackStrategy.fallbackMove(state, p1) match
            case Some(GameAction.PlayCard(_, c)) => c
            case other                           => fail(s"Unexpected: $other")
        wantWinPlays.count(_ == (Thirteen of Blue)) should be > 20

        val wantLosePlays = (1 to 40).map: i =>
          given Random = new Random(i.toLong)
          val state = GameState.Playing(core, Bids.empty + (p1 -> 0), table, p1, Tricks.empty)
          FallbackStrategy.fallbackMove(state, p1) match
            case Some(GameAction.PlayCard(_, c)) => c
            case other                           => fail(s"Unexpected: $other")
        wantLosePlays.count(_ != (Thirteen of Blue)) should be > 20

      "prefer strong cards when leading empty table and return single legal card" in:
        val cards = List(jester(0), wizard(0))
        val core = coreWith(cards, 2, Some(Two of Red))
        val wantWinPlays = (1 to 40).map: i =>
          given Random = new Random(i.toLong)
          val state =
            GameState.Playing(core, Bids.empty + (p1 -> 2), Table.empty, p1, Tricks.empty)
          FallbackStrategy.fallbackMove(state, p1) match
            case Some(GameAction.PlayCard(_, c)) => c
            case other                           => fail(s"Unexpected: $other")
        wantWinPlays.count(_ == wizard(0)) should be > 20

        val singleCore = coreWith(List(Five of Blue), 1)
        val singleState =
          GameState.Playing(singleCore, Bids.empty + (p1 -> 1), Table.empty, p1, Tricks.empty)
        FallbackStrategy.fallbackMove(singleState, p1) shouldBe Some(
          GameAction.PlayCard(p1, Five of Blue)
        )

      "return None on wrong turn or Ended state" in:
        val ended = GameState.Ended(players, Scoreboard.empty)
        FallbackStrategy.fallbackMove(ended, p1) shouldBe None
