package it.unibo.pps.wizard.engine.events

/** Base trait for all Wizard-specific game events. */
trait WizardEvent extends Event

import it.unibo.pps.wizard.engine.model.basic.PlayerId

/**
 * Marks an event as being specific to a single player.
 * Allows the UI to route the event to the correct player's client.
 */
trait PlayerScoped extends WizardEvent:
  def playerId: PlayerId
