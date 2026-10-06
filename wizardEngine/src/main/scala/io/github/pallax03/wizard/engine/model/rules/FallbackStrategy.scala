package io.github.pallax03.wizard.engine.model.rules

import scala.util.Random

import io.github.pallax03.wizard.engine.model.core.GameAction
import io.github.pallax03.wizard.engine.model.events.InvitationEvent

/**
 * Provides automated legal fallback actions when a player disconnects, times out (AFK),
 * or when a basic default move is needed.
 *
 * Guarantees that the generated [[io.github.pallax03.wizard.engine.model.core.GameAction]]
 * strictly adheres to the game rules for the pending [[InvitationEvent]].
 */
object FallbackStrategy:

  /** Generates a legal fallback [[GameAction]] for the given invitation event. */
  def fallbackMove(invitationEvent: InvitationEvent): GameAction = invitationEvent match
    case InvitationEvent.WaitingForTrump(playerId, colorOptions) =>
      GameAction.ResolveTrumpColor(playerId, colorOptions(Random.nextInt(colorOptions.length)))
    case InvitationEvent.WaitingForBid(playerId, round, invalidBid) =>
      val validBids = (0 to (round / 2) + 1).filterNot(b => invalidBid.contains(b))
      GameAction.PlaceBid(playerId, validBids(Random.nextInt(validBids.size)))
    case InvitationEvent.WaitingForCard(playerId, legalCards, _) =>
      GameAction.PlayCard(playerId, legalCards.head)
