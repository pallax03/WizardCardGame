package it.unibo.pps.wizard.engine.events

import it.unibo.pps.wizard.engine.model.basic.Players
import it.unibo.pps.wizard.engine.model.basic.scoreboard.Scoreboard
import it.unibo.pps.wizard.engine.model.configuration.BotsDifficulty

/** Represents high-level game lifecycle transitions. */
sealed trait LifecycleEvent extends WizardEvent

object LifecycleEvent:
  case class GameStarted(players: Players, botsDifficulty: BotsDifficulty) extends LifecycleEvent
  case class GameEnded(finalScores: Scoreboard, players: Players) extends LifecycleEvent
