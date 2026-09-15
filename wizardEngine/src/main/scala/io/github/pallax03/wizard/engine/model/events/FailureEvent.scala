package io.github.pallax03.wizard.engine.model.events

import io.github.pallax03.wizard.engine.model.basic.PlayerId
import io.github.pallax03.wizard.engine.model.core.GameActionError

/** Represents a failure during the processing of a game action. */
sealed trait FailureEvent extends WizardEvent, DestinationScoped

object FailureEvent:

  /**
   * Emitted when a requested action fails.
   *
   * @param playerId the player who attempted the invalid action.
   * @param reason   the specific [[GameActionError]] that caused the failure.
   */
  case class ActionFailed(playerId: PlayerId, reason: GameActionError) extends FailureEvent:
    override def destinationId: PlayerId = playerId
