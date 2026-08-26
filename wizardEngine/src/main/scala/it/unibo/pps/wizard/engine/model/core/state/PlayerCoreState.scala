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
  /**
   * Constructs a PlayerCoreState from a ServerCoreState by extracting the specific hand
   * of the given player. Hides the remaining players' hands.
   *
   * @param serverCore The current ServerCoreState.
   * @param playerId The ID of the player requesting the state.
   * @return A PlayerCoreState tailored for the specified player.
   * @throws GameException if the player's hand is not found in the server state,
   *                       indicating a corrupted state.
   */
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
