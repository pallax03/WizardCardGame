package it.unibo.pps.wizard.engine.model.core.state

import it.unibo.pps.wizard.engine.model.basic._
import it.unibo.pps.wizard.engine.model.basic.cards.Hand
import it.unibo.pps.wizard.engine.model.basic.gameplay.Round
import it.unibo.pps.wizard.engine.model.basic.gameplay.Trump
final case class PlayerCoreState(
    playersIds: List[PlayerId],
    hand: Hand,
    trump: Trump,
    round: Round,
    dealerId: PlayerId,
    scoreboard: Scoreboard
) extends CoreState

object PlayerCoreState:
  def from(serverCore: ServerCoreState, playerId: PlayerId): PlayerCoreState =
    val playerHand = serverCore.hands.getHand(playerId)
    PlayerCoreState(
      playersIds = serverCore.playersIds,
      hand = playerHand,
      trump = serverCore.trump,
      round = serverCore.round,
      dealerId = serverCore.dealerId,
      scoreboard = serverCore.scoreboard
    )
