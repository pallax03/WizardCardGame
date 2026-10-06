package io.github.pallax03.wizard.engine.model.events

import io.github.pallax03.wizard.engine.model.basic.PlayerId
import io.github.pallax03.wizard.engine.model.basic.bidding.Bid
import io.github.pallax03.wizard.engine.model.basic.cards.Card
import io.github.pallax03.wizard.engine.model.basic.gameplay.Round

/**
 * Represents an input prompt directed privately to the player whose turn it is.
 *
 * Implements [[DestinationScoped]] so that prompts containing private recommendations or legal move subsets
 * (such as legal cards in hand) are dispatched only to the active player.
 */
sealed trait InvitationEvent extends WizardEvent, DestinationScoped

object InvitationEvent:
  /**
   * Prompts the player to place their bid.
   *
   * @param invalidBid the forbidden bid value for the dealer under the last-player rule, if applicable.
   */
  case class WaitingForBid(destinationId: PlayerId, round: Round, invalidBid: Option[Bid] = None)
      extends InvitationEvent

  /**
   * Prompts the player to play a card.
   *
   * @param legalCards   the pre-filtered subset of cards in the player's hand that are legal to play.
   * @param isTableEmpty whether this player is leading the trick.
   */
  case class WaitingForCard(
      destinationId: PlayerId,
      legalCards: List[Card],
      isTableEmpty: Boolean
  ) extends InvitationEvent

  case class WaitingForTrump(
      destinationId: PlayerId,
      colorOptions: List[Card.Color] = Card.Color.values.toList
  ) extends InvitationEvent
