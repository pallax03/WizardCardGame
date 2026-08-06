package it.unibo.pps.wizard.engine.events

import it.unibo.pps.wizard.engine.model.basic.PlayerId
import it.unibo.pps.wizard.engine.model.basic.Scoreboard

/** Represents high-level game lifecycle transitions. */
sealed trait LifecycleEvent extends WizardEvent

object LifecycleEvent:
  case class GameStarted(playersIds: List[PlayerId]) extends LifecycleEvent
  case class GameEnded(finalScores: Scoreboard, playersIds: List[PlayerId]) extends LifecycleEvent
