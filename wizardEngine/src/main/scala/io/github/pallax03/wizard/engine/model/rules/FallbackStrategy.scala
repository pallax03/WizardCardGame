package io.github.pallax03.wizard.engine.model.rules

import io.github.pallax03.wizard.engine.model.basic.cards.Card
import io.github.pallax03.wizard.engine.model.core.GameAction
import io.github.pallax03.wizard.engine.model.events.InvitationEvent

import scala.util.Random

object FallbackStrategy:
  
  /** Returns a valid fallback [[GameAction]] (the simplest legal move) for the given invitation event. */
  def fallbackMove(invitationEvent: InvitationEvent): GameAction = invitationEvent match
    case InvitationEvent.WaitingForTrump(playerId) =>
      val colors = Card.Color.values
      GameAction.ResolveTrumpColor(playerId, colors(Random.nextInt(colors.length)))
    case InvitationEvent.WaitingForBid(playerId, round, invalidBid) =>
      val validBids = (0 to round).filterNot(b => invalidBid.contains(b))
      GameAction.PlaceBid(playerId, validBids(Random.nextInt(validBids.size)))
    case InvitationEvent.WaitingForCard(playerId, legalCards) => 
      GameAction.PlayCard(playerId, legalCards.head)
