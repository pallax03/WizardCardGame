package it.unibo.pps.wizard.engine.events

import it.unibo.pps.wizard.engine.model.basic.{PlayerId, Scoreboard}
import it.unibo.pps.wizard.engine.model.configuration.BotsDifficulty

/** Represents high-level game lifecycle transitions. */
sealed trait LifecycleEvent extends WizardEvent

object LifecycleEvent:
  case class GameStarted(playersIds: List[PlayerId], botsDifficulty: BotsDifficulty) extends LifecycleEvent
  case class GameEnded(finalScores: Scoreboard, playersIds: List[PlayerId]) extends LifecycleEvent
