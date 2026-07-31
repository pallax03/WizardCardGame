package it.unibo.pps.wizard.engine.events

import it.unibo.pps.wizard.engine.model.basic.PlayerId
import it.unibo.pps.wizard.engine.model.core.GameError

/** Represents a failure during the processing of a game action. */
sealed trait FailureEvent extends WizardEvent, PlayerScoped

object FailureEvent:

  /**
   * Emitted when a requested action fails.
   *
   * @param playerId the player who attempted the invalid action.
   * @param reason   the specific [[GameError]] that caused the failure.
   */
  case class ActionFailed(playerId: PlayerId, reason: GameError) extends FailureEvent
