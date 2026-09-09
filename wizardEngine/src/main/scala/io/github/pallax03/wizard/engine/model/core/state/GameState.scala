package io.github.pallax03.wizard.engine.model.core.state

import io.github.pallax03.wizard.engine.model.basic.*
import io.github.pallax03.wizard.engine.model.basic.bidding.{Bids, Tricks}
import io.github.pallax03.wizard.engine.model.basic.gameplay.Table
import io.github.pallax03.wizard.engine.model.events.InvitationEvent
import io.github.pallax03.wizard.engine.model.rules.BiddingRules.notValidBid
import io.github.pallax03.wizard.engine.model.rules.TableRules.legalCards

/** Represents the various phases and states of the Wizard card game. */
sealed trait GameState[+C <: CoreState] extends Product:
  def playersIds: List[PlayerId] = this match
    case GameState.ChoosingTrump(core)       => core.playersIds
    case GameState.Bidding(core, _, _)       => core.playersIds
    case GameState.Playing(core, _, _, _, _) => core.playersIds
    case GameState.Ended(ids, _)             => ids

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

  case class Ended(override val playersIds: List[PlayerId], scoreboard: Scoreboard)
      extends GameState[Nothing]

  extension [C <: CoreState](state: GameState[C])
    /** Deduces the pending invitation event for a given player based on the current state. */
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
            InvitationEvent.WaitingForCard(playerId, core.hands.getHand(playerId).legalCards(table))
          )
        case GameState.Playing(core: PlayerCoreState, _, table, turn, _) if turn == playerId =>
          Some(
            InvitationEvent.WaitingForCard(playerId, core.hand.legalCards(table))
          )
        case GameState.ChoosingTrump(core) if core.dealerId == playerId =>
          Some(InvitationEvent.WaitingForTrump(playerId))
        case _ => None

type ServerGameState = GameState[ServerCoreState]
