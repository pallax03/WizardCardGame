package io.github.pallax03.wizard.engine.model.events

import io.github.pallax03.wizard.engine.model.basic.*
import io.github.pallax03.wizard.engine.model.basic.bidding.Bid
import io.github.pallax03.wizard.engine.model.basic.cards.Card

/**
 * Represents successful game actions performed by players, broadcast to all participants.
 *
 * @see [[io.github.pallax03.wizard.engine.model.core.GameAction]] for the originating client intents.
 */
sealed trait ActionEvent extends WizardEvent, PlayerScoped

object ActionEvent:
  case class TrumpColorResolved(playerId: PlayerId, color: Card.Color) extends ActionEvent

  /**
   * Broadcast when a player plays a card, including trick status updates.
   *
   * @param winningCard    the card currently winning the trick after this move.
   * @param followingColor the lead suit subsequent players must follow, if established.
   */
  case class CardPlayed(
      playerId: PlayerId,
      card: Card,
      winningCard: Option[Card],
      followingColor: Option[Card.Color]
  ) extends ActionEvent

  case class BidPlaced(playerId: PlayerId, bid: Bid) extends ActionEvent
