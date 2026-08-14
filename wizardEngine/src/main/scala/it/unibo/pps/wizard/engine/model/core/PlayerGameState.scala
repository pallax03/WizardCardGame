package it.unibo.pps.wizard.engine.model.core

import it.unibo.pps.wizard.engine.model.basic.PlayerId
import it.unibo.pps.wizard.engine.model.basic.cards.Card
import it.unibo.pps.wizard.engine.model.core.InconsistentStateReasons.HandNotFoundFor

type PlayerGameState = GameState

// todo: wait until InvitationEvents and bots are fully working
object PlayerGameState:
  def apply(gameState: GameState)(playerId: PlayerId): PlayerGameState = gameState match
    case GameState.ChoosingTrump(core) => ???
    case GameState.Bidding(core, bids, playerTurn) => ???
    case GameState.Playing(core, bids, table, playerTurn, tricksWon) => ???
    case GameState.Ended(playersIds, scoreboard) => ???

  def extractHand(coreState: CoreState)(playerId: PlayerId): Either[GameError, List[Card]] =
    coreState.hands.getHand(playerId).map(_.toList).toRight(GameError.InconsistentState(HandNotFoundFor(playerId)))