package io.github.pallax03.wizard.engine.model.events

import io.github.pallax03.wizard.engine.model.basic.PlayerId
import io.github.pallax03.wizard.engine.model.basic.bidding.Bid
import io.github.pallax03.wizard.engine.model.basic.cards.Card
import io.github.pallax03.wizard.engine.model.basic.gameplay.Round

/** Represents a request for input for a specific player. */
sealed trait InvitationEvent extends WizardEvent, PlayerScoped

object InvitationEvent:
  case class WaitingForBid(playerId: PlayerId, round: Round, invalidBid: Option[Bid] = None) extends InvitationEvent
  case class WaitingForCard(playerId: PlayerId, legalCards: List[Card]) extends InvitationEvent
  case class WaitingForTrump(playerId: PlayerId) extends InvitationEvent
