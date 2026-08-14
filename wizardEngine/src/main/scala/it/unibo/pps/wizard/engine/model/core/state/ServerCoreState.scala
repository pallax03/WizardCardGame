package it.unibo.pps.wizard.engine.model.core.state

import it.unibo.pps.wizard.engine.model.basic.*
import it.unibo.pps.wizard.engine.model.basic.cards.Hands
import it.unibo.pps.wizard.engine.model.basic.gameplay.{Round, Trump}
import it.unibo.pps.wizard.engine.model.core.{GameError, InconsistentStateReasons}
import it.unibo.pps.wizard.engine.model.rules.RoundManager.firstPlayer

case class ServerCoreState(
    playersIds: List[PlayerId],
    hands: Hands,
    trump: Trump,
    round: Round,
    dealerId: PlayerId,
    scoreboard: Scoreboard
):
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

  extension (hands: Hands)
    def getHandSafe(player: PlayerId): Either[GameError, it.unibo.pps.wizard.engine.model.basic.cards.Hand] =
      hands.getHand(player).toRight(GameError.InconsistentState(InconsistentStateReasons.HandNotFoundFor(player)))
      
    def removeSafe(player: PlayerId, card: it.unibo.pps.wizard.engine.model.basic.cards.Card): Either[GameError, Hands] =
      hands.remove(player, card).toRight(GameError.InconsistentState(InconsistentStateReasons.HandNotFoundFor(player)))
