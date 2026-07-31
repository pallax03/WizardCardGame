package it.unibo.pps.wizard.engine.events

import it.unibo.pps.wizard.engine.model.basic.PlayerId
import it.unibo.pps.wizard.engine.model.basic.cards.Card
import it.unibo.pps.wizard.engine.model.basic.gameplay.Round

/** Represents a request for input for a specific player. */
sealed trait InvitationEvent extends WizardEvent, PlayerScoped

object InvitationEvent:
  case class WaitingForBid(playerId: PlayerId, round: Round) extends InvitationEvent
  case class WaitingForCard(playerId: PlayerId, legalCards: List[Card]) extends InvitationEvent
  case class WaitingForTrump(playerId: PlayerId) extends InvitationEvent
