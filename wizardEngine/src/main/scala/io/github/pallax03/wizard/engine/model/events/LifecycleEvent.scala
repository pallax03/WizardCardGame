package io.github.pallax03.wizard.engine.model.events

import io.github.pallax03.wizard.engine.model.basic.PlayerId
import io.github.pallax03.wizard.engine.model.basic.Scoreboard

/** Represents high-level game lifecycle transitions. */
sealed trait LifecycleEvent extends WizardEvent

object LifecycleEvent:
  case class GameStarted(playersIds: List[PlayerId]) extends LifecycleEvent
  case class GameEnded(playersIds: List[PlayerId], finalScores: Scoreboard) extends LifecycleEvent
