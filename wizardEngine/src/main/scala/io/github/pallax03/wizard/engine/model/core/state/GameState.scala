package io.github.pallax03.wizard.engine.model.core.state

import io.github.pallax03.wizard.engine.model.basic.*
import io.github.pallax03.wizard.engine.model.basic.bidding.{Bids, Tricks}
import io.github.pallax03.wizard.engine.model.basic.gameplay.Table
import io.github.pallax03.wizard.engine.model.events.InvitationEvent
import io.github.pallax03.wizard.engine.model.rules.TableRules.legalCards
import io.github.pallax03.wizard.engine.model.rules.BiddingRules.notValidBid

/** Represents the various phases and states of the Wizard card game. */
sealed trait GameState[+C <: CoreState]:
  def playersIds: List[PlayerId] = this match
    case GameState.ChoosingTrump(core) => core.playersIds
    case GameState.Bidding(core, _, _) => core.playersIds
    case GameState.Playing(core, _, _, _, _) => core.playersIds
    case GameState.Ended(ids, _) => ids

object GameState:

  case class ChoosingTrump[C <: CoreState](
      core: C
  ) extends GameState[C]

  case class Bidding[C <: CoreState](core: C, bids: Bids, playerTurn: PlayerId) extends GameState[C]

  case class Playing[C <: CoreState](
      core: C,
      bids: Bids,
      table: Table,
      playerTurn: PlayerId,
      tricksWon: Tricks
  ) extends GameState[C]

  case class Ended(override val playersIds: List[PlayerId], scoreboard: Scoreboard) extends GameState[Nothing]

  extension [C <: CoreState](state: GameState[C])
    /**
     * Deduces the pending invitation event for a given player based on the current state.
     */
    def pendingInvitation(playerId: PlayerId): Option[InvitationEvent] =
      state match
        case GameState.Bidding(core, bids, turn) if turn == playerId =>
          Some(InvitationEvent.WaitingForBid(playerId, core.round, bids.notValidBid(core.round, core.playersIds.size)))
        case GameState.Playing(core: PlayerCoreState, _, _, turn, _) if turn == playerId =>
          Some(InvitationEvent.WaitingForCard(playerId, core.hand.toList))
        case GameState.Playing(core: ServerCoreState, _, table, turn, _) if turn == playerId =>
          Some(InvitationEvent.WaitingForCard(playerId, core.hands.getHand(playerId).legalCards(table)))
        case GameState.ChoosingTrump(core) if core.dealerId == playerId =>
          Some(InvitationEvent.WaitingForTrump(playerId))
        case _ => None

type ServerGameState = GameState[ServerCoreState]
type PlayerGameState = GameState[PlayerCoreState]

object PlayerGameState:
  /**
   * Translates a ServerGameState into a PlayerGameState.
   * This limits the state visibility to only what the specified player is allowed to see
   * (e.g., hiding other players' hands).
   *
   * @param serverGameState The complete server-side game state.
   * @param playerId The ID of the player requesting the state.
   * @return A restricted PlayerGameState tailored for the specified player.
   * @throws GameException if an inconsistency is detected (e.g. the player's hand is missing),
   *                       indicating that the state is corrupted.
   */
  def from(
      serverGameState: ServerGameState,
      playerId: PlayerId
  ): PlayerGameState =
    serverGameState match
      case GameState.ChoosingTrump(core) =>
        GameState.ChoosingTrump(PlayerCoreState.from(core, playerId))
      case GameState.Bidding(core, bids, turn) =>
        GameState.Bidding(PlayerCoreState.from(core, playerId), bids, turn)
      case GameState.Playing(core, bids, table, turn, tricks) =>
        GameState.Playing(PlayerCoreState.from(core, playerId), bids, table, turn, tricks)
      case GameState.Ended(playersIds, scoreboard) =>
        GameState.Ended(playersIds, scoreboard)