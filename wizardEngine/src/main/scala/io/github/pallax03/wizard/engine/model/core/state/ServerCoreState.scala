package io.github.pallax03.wizard.engine.model.core.state

import io.github.pallax03.wizard.engine.model.basic._
import io.github.pallax03.wizard.engine.model.basic.cards.Hands
import io.github.pallax03.wizard.engine.model.basic.gameplay.Round
import io.github.pallax03.wizard.engine.model.basic.gameplay.Trump
import io.github.pallax03.wizard.engine.model.rules.RoundManager.firstPlayer

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
