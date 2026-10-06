package io.github.pallax03.wizard.engine.model.core.state

import io.github.pallax03.wizard.engine.model.basic.*
import io.github.pallax03.wizard.engine.model.basic.gameplay.{Round, Trump}

/**
 * Common abstraction representing the fundamental shared state of a Wizard match round.
 *
 * Specialized by:
 *   - [[ServerCoreState]]: complete server-side state holding all players' hands.
 *   - [[PlayerCoreState]]: projection filtered for a specific player containing only their own hand.
 */
trait CoreState:
  def playersIds: List[PlayerId]
  def trump: Trump
  def round: Round
  def dealerId: PlayerId
  def scoreboard: Scoreboard
