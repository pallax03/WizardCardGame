package io.github.pallax03.wizard.engine.model.events

import io.github.pallax03.wizard.engine.model.basic.PlayerId
import io.github.pallax03.wizard.engine.model.basic.bidding.Bid
import io.github.pallax03.wizard.engine.model.basic.cards.Card
import io.github.pallax03.wizard.engine.model.basic.gameplay.Round

/** Represents a request for input for a specific player. */
sealed trait InvitationEvent extends WizardEvent, DestinationScoped

object InvitationEvent:
  case class WaitingForBid(destinationId: PlayerId, round: Round, invalidBid: Option[Bid] = None)
      extends InvitationEvent
  case class WaitingForCard(destinationId: PlayerId, legalCards: List[Card]) extends InvitationEvent
  case class WaitingForTrump(destinationId: PlayerId, colorOptions: List[Card.Color] = Card.Color.values.toList) extends InvitationEvent
