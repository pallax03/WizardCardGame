package it.unibo.pps.wizard.engine.model.events

import it.unibo.pps.wizard.engine.model.basic._
import it.unibo.pps.wizard.engine.model.basic.bidding.Trick
import it.unibo.pps.wizard.engine.model.basic.cards.Card
import it.unibo.pps.wizard.engine.model.basic.cards.Hands
import it.unibo.pps.wizard.engine.model.basic.gameplay.Round
import it.unibo.pps.wizard.engine.model.basic.gameplay.Trump

/** Represents game progress updates, such as phase changes or round results. */
sealed trait ProgressEvent extends WizardEvent

object ProgressEvent:
  case class CardsDealt(playerId: PlayerId, hands: Hands, trump: Trump, round: Round)
      extends ProgressEvent, DestinationScoped:
    override def destinationId: PlayerId = playerId
  case class TrickWon(winnerId: PlayerId, tricksWon: Trick, trickedCards: List[Card])
      extends ProgressEvent, PlayerScoped:
    override def playerId: PlayerId = winnerId
  case class RoundScored(playersIds: List[PlayerId], scoreboard: Scoreboard) extends ProgressEvent
  case class PhaseChanged(phaseName: String) extends ProgressEvent
