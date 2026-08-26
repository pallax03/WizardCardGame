package io.github.pallax03.wizard.engine.model.core.state

import io.github.pallax03.wizard.engine.model.basic._
import io.github.pallax03.wizard.engine.model.basic.gameplay.Round
import io.github.pallax03.wizard.engine.model.basic.gameplay.Trump

trait CoreState:
  def playersIds: List[PlayerId]
  def trump: Trump
  def round: Round
  def dealerId: PlayerId
  def scoreboard: Scoreboard
