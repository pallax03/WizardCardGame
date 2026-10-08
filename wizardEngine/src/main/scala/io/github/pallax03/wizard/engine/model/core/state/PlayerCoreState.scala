package io.github.pallax03.wizard.engine.model.core.state

import io.github.pallax03.wizard.engine.model.basic.*
import io.github.pallax03.wizard.engine.model.basic.cards.Hand
import io.github.pallax03.wizard.engine.model.basic.gameplay.{Round, Trump}

/**
 * Client-facing core state projection filtered for a specific player.
 *
 * Exposes only the player's own [[Hand]], strictly hiding the cards held by opponents.
 */
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
   * Constructs a [[PlayerCoreState]] from an authoritative [[ServerCoreState]] by extracting
   * the specific hand of the given player and omitting opponents' hands.
   *
   * @param serverCore the current complete server-side core state.
   * @param playerId   the ID of the player requesting the state view.
   * @return a [[PlayerCoreState]] containing only the requested player's hand.
   * @throws GameException.CorruptedHand if the player's hand is not found in the server state.
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
