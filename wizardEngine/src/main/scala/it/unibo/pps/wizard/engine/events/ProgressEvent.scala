package it.unibo.pps.wizard.engine.events

import it.unibo.pps.wizard.engine.model.basic.*
import it.unibo.pps.wizard.engine.model.basic.bidding.Trick
import it.unibo.pps.wizard.engine.model.basic.cards.Card
import it.unibo.pps.wizard.engine.model.basic.cards.Hands
import it.unibo.pps.wizard.engine.model.basic.gameplay.Round
import it.unibo.pps.wizard.engine.model.basic.gameplay.Trump

/** Represents game progress updates, such as phase changes or round results. */
sealed trait ProgressEvent extends WizardEvent

object ProgressEvent:
  case class CardsDealt(playerId: PlayerId, hands: Hands, trump: Trump, round: Round)
      extends ProgressEvent
  case class TrickWon(winnerId: PlayerId, tricksWon: Trick, trickedCards: List[Card])
      extends ProgressEvent
  case class RoundScored(scoreboard: Scoreboard, playersIds: List[PlayerId]) extends ProgressEvent
  case class PhaseChanged(phaseName: String) extends ProgressEvent
