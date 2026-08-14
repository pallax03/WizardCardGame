package it.unibo.pps.wizard.engine.model.core.state

import it.unibo.pps.wizard.engine.model.basic.*
import it.unibo.pps.wizard.engine.model.basic.cards.Hand
import it.unibo.pps.wizard.engine.model.basic.gameplay.{Round, Trump}
import it.unibo.pps.wizard.engine.model.core.GameError

case class PlayerCoreState(
    playersIds: List[PlayerId],
    hand: Hand,
    trump: Trump,
    round: Round,
    dealerId: PlayerId,
    scoreboard: Scoreboard
)

object PlayerCoreState:
  def from(serverCore: ServerCoreState, playerId: PlayerId): Either[GameError, PlayerCoreState] =
    import ServerCoreState.*
    serverCore.hands.getHandSafe(playerId).map: playerHand =>
      PlayerCoreState(
        playersIds = serverCore.playersIds,
        hand = playerHand,
        trump = serverCore.trump,
        round = serverCore.round,
        dealerId = serverCore.dealerId,
        scoreboard = serverCore.scoreboard
      )