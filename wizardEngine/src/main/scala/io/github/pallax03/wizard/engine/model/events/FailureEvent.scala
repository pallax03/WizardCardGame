package io.github.pallax03.wizard.engine.model.events

import io.github.pallax03.wizard.engine.model.basic.PlayerId
import io.github.pallax03.wizard.engine.model.core.GameActionError

/**
 * Represents a failure notification during the processing of an action, sent privately to the offending player.
 *
 * Implements [[DestinationScoped]] to prevent leaking error details or hand validation metadata to other players.
 */
sealed trait FailureEvent extends WizardEvent, DestinationScoped

object FailureEvent:

  /**
   * Emitted privately when a player's requested action fails validation against game rules.
   *
   * @param playerId the player who attempted the invalid action.
   * @param reason   the specific [[GameActionError]] explaining why the action was rejected.
   */
  case class ActionFailed(playerId: PlayerId, reason: GameActionError) extends FailureEvent:
    override def destinationId: PlayerId = playerId
