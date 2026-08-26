package io.github.pallax03.wizard.engine.model.events

import io.github.pallax03.wizard.engine.model.basic._
import io.github.pallax03.wizard.engine.model.basic.bidding.Bid
import io.github.pallax03.wizard.engine.model.basic.cards.Card

/** Represents successful game actions performed by players. */
sealed trait ActionEvent extends WizardEvent, PlayerScoped

object ActionEvent:
  /** Emitted when the dealer resolves a Wizard trump card into a color. */
  case class TrumpColorResolved(playerId: PlayerId, color: Card.Color) extends ActionEvent
  case class CardPlayed(
      playerId: PlayerId,
      card: Card,
      winningCard: Option[Card],
      followingColor: Option[Card.Color]
  ) extends ActionEvent
  case class BidPlaced(playerId: PlayerId, bid: Bid) extends ActionEvent
