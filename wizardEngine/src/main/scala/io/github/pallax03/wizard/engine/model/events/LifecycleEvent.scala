package io.github.pallax03.wizard.engine.model.events

import io.github.pallax03.wizard.engine.model.basic.{PlayerId, Scoreboard}

/** Represents high-level game lifecycle transitions. */
sealed trait LifecycleEvent extends WizardEvent

object LifecycleEvent:
  case class GameStarted(playersIds: List[PlayerId]) extends LifecycleEvent
  case class GameResumed(playersIds: List[PlayerId]) extends LifecycleEvent
  case class GameEnded(playersIds: List[PlayerId], finalScores: Scoreboard) extends LifecycleEvent
  case object StateRecovered extends LifecycleEvent
  case class GameCancelled(reason: Option[String] = None) extends LifecycleEvent
