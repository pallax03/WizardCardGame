package io.github.pallax03.wizard.engine.model.rules

import io.github.pallax03.wizard.engine.model.basic.*
import io.github.pallax03.wizard.engine.model.core.GameAction
import io.github.pallax03.wizard.engine.model.events.InvitationEvent

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class TestFallbackStrategy extends AnyWordSpec with Matchers:
  import cards.Card

  "FallbackStrategy" when:
    val p1 = PlayerId(1)

    "handling WaitingForTrump" should:
      "return a ResolveTrumpColor action with a valid color" in:
        FallbackStrategy.fallbackMove(InvitationEvent.WaitingForTrump(p1)) match
          case GameAction.ResolveTrumpColor(playerId, color) =>
            playerId shouldBe p1
            Card.Color.values should contain(color)
          case _ => fail("Expected ResolveTrumpColor")

    "handling WaitingForBid" should:
      "return a PlaceBid action that avoids invalid bids" in:
        val round = 3
        val invalidBid = Some(1)
        FallbackStrategy.fallbackMove(InvitationEvent.WaitingForBid(p1, round, invalidBid)) match
          case GameAction.PlaceBid(playerId, bid) =>
            playerId shouldBe p1
            bid should be >= 0
            bid should be <= round
            bid should not be 1
          case _ => fail("Expected PlaceBid")

    "handling WaitingForCard" should:
      "return a PlayCard action picking the first legal card" in:
        val cards = (One of Red) - jester
        FallbackStrategy.fallbackMove(InvitationEvent.WaitingForCard(p1, cards)) match
          case GameAction.PlayCard(playerId, card) =>
            playerId shouldBe p1
            card shouldBe cards.head
          case _ => fail("Expected PlayCard")
