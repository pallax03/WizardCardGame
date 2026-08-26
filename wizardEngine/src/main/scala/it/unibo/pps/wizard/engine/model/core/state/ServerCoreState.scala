package it.unibo.pps.wizard.engine.model.core.state

import it.unibo.pps.wizard.engine.model.basic._
import it.unibo.pps.wizard.engine.model.basic.cards.Hands
import it.unibo.pps.wizard.engine.model.basic.gameplay.Round
import it.unibo.pps.wizard.engine.model.basic.gameplay.Trump
import it.unibo.pps.wizard.engine.model.rules.RoundManager.firstPlayer

final case class ServerCoreState(
    playersIds: List[PlayerId],
    hands: Hands,
    trump: Trump,
    round: Round,
    dealerId: PlayerId,
    scoreboard: Scoreboard
) extends CoreState:
  def updateTrump(trump: Trump): ServerCoreState = this.copy(trump = trump)

object ServerCoreState:
  def initialize(
      playersIds: List[PlayerId],
      round: Round
  ): ServerCoreState =
    ServerCoreState(
      playersIds = playersIds,
      hands = Hands.empty,
      trump = Trump.Absent,
      round = round,
      dealerId = round.firstPlayer(playersIds),
      scoreboard = Scoreboard.empty
    )
