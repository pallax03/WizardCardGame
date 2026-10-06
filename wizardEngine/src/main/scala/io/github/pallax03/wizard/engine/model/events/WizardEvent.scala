package io.github.pallax03.wizard.engine.model.events

/**
 * Root marker trait for all domain events emitted by the Wizard game engine.
 */
trait WizardEvent

import io.github.pallax03.wizard.engine.model.basic.PlayerId

/**
 * Marks a broadcast event that pertains to a specific player.
 *
 * This event is broadcast to all players in the room, informing them about an action,
 * turn transition, or state change concerning the designated player.
 *
 * Example: [[ProgressEvent.TurnOf]] informs everyone whose turn it is.
 */
trait PlayerScoped extends WizardEvent:
  def playerId: PlayerId

/**
 * Marks a confidential or unicast event that must be routed exclusively to a specific player.
 *
 * Unlike [[PlayerScoped]], events implementing [[DestinationScoped]] contain private information
 * or direct invitations (e.g. a player's dealt cards or private action failure notifications)
 * and must not be broadcast to opponents.
 *
 * Example: [[ProgressEvent.CardsDealt]], [[InvitationEvent.WaitingForCard]].
 */
trait DestinationScoped extends WizardEvent:
  def destinationId: PlayerId
