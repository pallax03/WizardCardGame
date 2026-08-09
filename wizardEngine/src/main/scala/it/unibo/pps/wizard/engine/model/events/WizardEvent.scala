package it.unibo.pps.wizard.engine.model.events

/** Base trait for all Wizard-specific game events. */
trait WizardEvent

import it.unibo.pps.wizard.engine.model.basic.PlayerId

/**
 * Marks a global event as being specific to a single player.
 * Inform every player that playerId trigger a FailureEvent or a InvitationEvent.
 * Example: InvitationEvent: WaitingForBid for playerId2 -> inform every player that playerId2 need to place a bid.
 */
trait PlayerScoped extends WizardEvent:
  def playerId: PlayerId

/**
 * Marks an event as highly sensitive/private, to be delivered ONLY to the specified player.
 * Differs from playerScoped, THIS IS A PRIVATE EVENT SENT ONLY TO THE SPECIFIED PlayerId.
 * Example: Dealing cards to a player's hand.
 */
trait DestinationScoped extends WizardEvent:
  def destinationId: PlayerId
