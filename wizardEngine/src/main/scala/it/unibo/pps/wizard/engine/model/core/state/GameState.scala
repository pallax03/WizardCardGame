package it.unibo.pps.wizard.engine.model.core.state

import it.unibo.pps.wizard.engine.model.basic._
import it.unibo.pps.wizard.engine.model.basic.bidding.Bids
import it.unibo.pps.wizard.engine.model.basic.bidding.Tricks
import it.unibo.pps.wizard.engine.model.basic.gameplay.Table
import it.unibo.pps.wizard.engine.model.core.GameError

/** Represents the various phases and states of the Wizard card game. */
sealed trait GameState[+C]

object GameState:

  case class ChoosingTrump[C](
      core: C
  ) extends GameState[C]

  case class Bidding[C](core: C, bids: Bids, playerTurn: PlayerId) extends GameState[C]

  case class Playing[C](
      core: C,
      bids: Bids,
      table: Table,
      playerTurn: PlayerId,
      tricksWon: Tricks
  ) extends GameState[C]

  case class Ended(playersIds: List[PlayerId], scoreboard: Scoreboard) extends GameState[Nothing]

type ServerGameState = GameState[ServerCoreState]
type PlayerGameState = GameState[PlayerCoreState]

object PlayerGameState:
  def from(
      serverGameState: ServerGameState,
      playerId: PlayerId
  ): Either[GameError, PlayerGameState] =
    serverGameState match
      case GameState.ChoosingTrump(core) =>
        PlayerCoreState.from(core, playerId).map(GameState.ChoosingTrump(_))
      case GameState.Bidding(core, bids, turn) =>
        PlayerCoreState.from(core, playerId).map(GameState.Bidding(_, bids, turn))
      case GameState.Playing(core, bids, table, turn, tricks) =>
        PlayerCoreState.from(core, playerId).map(GameState.Playing(_, bids, table, turn, tricks))
      case GameState.Ended(playersIds, scoreboard) =>
        Right(GameState.Ended(playersIds, scoreboard))
