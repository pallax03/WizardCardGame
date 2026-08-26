package it.unibo.pps.wizard.engine.model.core.state

import it.unibo.pps.wizard.engine.model.basic._
import it.unibo.pps.wizard.engine.model.basic.gameplay.Round
import it.unibo.pps.wizard.engine.model.basic.gameplay.Trump

trait CoreState:
  def playersIds: List[PlayerId]
  def trump: Trump
  def round: Round
  def dealerId: PlayerId
  def scoreboard: Scoreboard
