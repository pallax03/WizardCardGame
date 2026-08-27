package io.github.pallax03.wizard.engine.model.events

import io.github.pallax03.wizard.engine.model.basic.PlayerId
import io.github.pallax03.wizard.engine.model.core.GameError

/** Represents a failure during the processing of a game action. */
sealed trait FailureEvent extends WizardEvent, DestinationScoped

object FailureEvent:

  /**
   * Emitted when a requested action fails.
   *
   * @param playerId the player who attempted the invalid action.
   * @param reason   the specific [[GameError]] that caused the failure.
   */
  case class ActionFailed(playerId: PlayerId, reason: GameError) extends FailureEvent:
    override def destinationId: PlayerId = playerId
