package io.github.pallax03.wizard.engine.model.core.state

import io.github.pallax03.wizard.engine.model.basic.*
import io.github.pallax03.wizard.engine.model.basic.bidding.{Bids, Tricks}
import io.github.pallax03.wizard.engine.model.basic.gameplay.Table
import io.github.pallax03.wizard.engine.model.events.InvitationEvent
import io.github.pallax03.wizard.engine.model.rules.BiddingRules.notValidBid
import io.github.pallax03.wizard.engine.model.rules.TableRules.legalCards

/**
 * Algebraic Data Type representing the phases and progression states of a Wizard match.
 *
 * Parameterized by `C <: CoreState` to allow type-safe representation of both the authoritative
 * server-side view ([[ServerCoreState]]) and the sanitized client-side view ([[PlayerCoreState]]).
 *
 * @tparam C the concrete [[CoreState]] implementation stored in this state.
 */
sealed trait GameState[+C <: CoreState] extends Product:
  /** Returns the list of participating players in the match. */
  def playersIds: List[PlayerId] = this match
    case GameState.ChoosingTrump(core)       => core.playersIds
    case GameState.Bidding(core, _, _)       => core.playersIds
    case GameState.Playing(core, _, _, _, _) => core.playersIds
    case GameState.Ended(ids, _)             => ids

object GameState:
  case class ChoosingTrump[C <: CoreState](core: C) extends GameState[C]
  case class Bidding[C <: CoreState](core: C, bids: Bids, playerTurn: PlayerId) extends GameState[C]
  case class Playing[C <: CoreState](
      core: C,
      bids: Bids,
      table: Table,
      playerTurn: PlayerId,
      tricksWon: Tricks
  ) extends GameState[C]
  case class Ended(override val playersIds: List[PlayerId], scoreboard: Scoreboard)
      extends GameState[Nothing]

  extension [C <: CoreState](state: GameState[C])
    /**
     * Deduces the pending [[InvitationEvent]] targeted at the specified player in the current state.
     *
     * Used by the server to dispatch prompts to clients or calculate fallback actions upon timeout.
     *
     * @param playerId the player to check for pending invitations.
     * @return `Some(invitation)` if this player is currently expected to act, or `None` otherwise.
     */
    def pendingInvitation(playerId: PlayerId): Option[InvitationEvent] =
      state match
        case GameState.Bidding(core, bids, turn) if turn == playerId =>
          Some(
            InvitationEvent.WaitingForBid(
              playerId,
              core.round,
              bids.notValidBid(core.round, core.playersIds.size)
            )
          )
        case GameState.Playing(core: ServerCoreState, _, table, turn, _) if turn == playerId =>
          Some(
            InvitationEvent.WaitingForCard(
              playerId,
              core.hands.getHand(playerId).legalCards(table),
              table.playedCards.isEmpty
            )
          )
        case GameState.Playing(core: PlayerCoreState, _, table, turn, _) if turn == playerId =>
          Some(
            InvitationEvent.WaitingForCard(
              playerId,
              core.hand.legalCards(table),
              table.playedCards.isEmpty
            )
          )
        case GameState.ChoosingTrump(core) if core.dealerId == playerId =>
          Some(InvitationEvent.WaitingForTrump(playerId))
        case _ => None

/** Authoritative server-side game state wrapping [[ServerCoreState]]. */
type ServerGameState = GameState[ServerCoreState]
