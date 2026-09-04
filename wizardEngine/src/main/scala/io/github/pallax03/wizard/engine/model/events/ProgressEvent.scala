package io.github.pallax03.wizard.engine.model.events

import io.github.pallax03.wizard.engine.model.basic.*
import io.github.pallax03.wizard.engine.model.basic.bidding.Trick
import io.github.pallax03.wizard.engine.model.basic.cards.{Card, Hand}
import io.github.pallax03.wizard.engine.model.basic.gameplay.{Round, Trump}

/** Represents game progress updates, such as phase changes or round results. */
sealed trait ProgressEvent extends WizardEvent

object ProgressEvent:
  case class CardsDealt(playerId: PlayerId, hand: Hand, trump: Trump, round: Round)
      extends ProgressEvent,
        DestinationScoped:
    override def destinationId: PlayerId = playerId
  case class TrickWon(winnerId: PlayerId, tricksWon: Trick, trickedCards: List[Card])
      extends ProgressEvent,
        PlayerScoped:
    override def playerId: PlayerId = winnerId
  case class RoundScored(playersIds: List[PlayerId], scoreboard: Scoreboard) extends ProgressEvent
  case class PhaseChanged(phaseName: String) extends ProgressEvent
