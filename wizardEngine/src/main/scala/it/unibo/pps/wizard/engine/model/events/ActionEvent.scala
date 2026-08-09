package it.unibo.pps.wizard.engine.model.events

import it.unibo.pps.wizard.engine.model.basic._
import it.unibo.pps.wizard.engine.model.basic.bidding.Bid
import it.unibo.pps.wizard.engine.model.basic.cards.Card

/** Represents successful game actions performed by players. */
sealed trait ActionEvent extends WizardEvent

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
