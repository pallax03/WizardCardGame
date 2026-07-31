package it.unibo.pps.wizard.engine.model.rules

import cats.data.State
import it.unibo.pps.wizard.engine.model.basic.PlayerId
import it.unibo.pps.wizard.engine.model.basic.Players
import it.unibo.pps.wizard.engine.model.basic.bidding.Bids
import it.unibo.pps.wizard.engine.model.basic.cards._
import it.unibo.pps.wizard.engine.model.basic.gameplay._
import it.unibo.pps.wizard.engine.model.core.CoreState
import it.unibo.pps.wizard.engine.model.core.GameError
import it.unibo.pps.wizard.engine.model.core.GameState

/** Manages game round lifecycle operations, player turns, card dealing, and state initialization. */
object RoundManager:

  extension (players: Players)
    /**
     * Determines the next player in the turn order following the current player.
     *
     * @param current the ID of the current player.
     * @return Right containing the next [[PlayerId]], or Left with a [[GameError]] if the player is not found.
     */
    def nextAfter(current: PlayerId): Either[GameError, PlayerId] =
      val idx = players.toList.indexWhere(_.id == current)
      Either.cond(
        idx >= 0,
        players.toList((idx + 1) % players.totalPlayers).id,
        GameError.NotYourTurn
      )

  extension (round: Round)
    /**
     * Determines the first player of the given round based on a shifting rotation.
     *
     * @param players the list of players.
     * @return the [[PlayerId]] of the dealer's successor who starts the round.
     */
    def firstPlayer(players: Players): PlayerId =
      players.toList((round.value - 1) % players.totalPlayers).id

    /**
     * Checks if this round is the final round of the game based on the player count.
     *
     * @param players the list of players.
     * @return true if all cards in the deck are distributed evenly, false otherwise.
     */
    def isLastRound(players: Players): Boolean =
      round.value == (Deck.TOTAL_SIZE / players.totalPlayers)

    /**
     * State action that deals cards to players and reveals the trump card.
     *
     * @param players the list of players.
     * @return a state transition resulting in a tuple of [[Hands]] and an optional trump [[Card]].
     */
    def deal(players: Players): State[Deck, (Hands, Option[Card])] =
      val cardsPerPlayer = round.value
      for
        drawn <- Deck.pop(cardsPerPlayer * players.totalPlayers)
        hands = Hands(
          players.toList.map(_.id).zip(drawn.grouped(cardsPerPlayer).map(Hand(_)).toList).toMap
        )
        trump <- Deck.pop(1).map(_.headOption)
      yield (hands, trump)

    /**
     * State action that initializes a new round, dealing cards and determining the next phase.
     * Transitions to either choosing a trump or the bidding phase.
     *
     * @param deck the current [[Deck]] to draw from.
     * @return a state transition resulting in the initial [[GameState]] for the round.
     */
    def initialize(deck: Deck): State[CoreState, GameState] =
      for
        core <- State.get[CoreState]

        (hands, optionTrump) = round.deal(core.players).runA(deck).value

        firstPlayer = round.firstPlayer(core.players)

        newCore = core.copy(
          hands = hands,
          trump = optionTrump.asTrump
        )

        _ <- State.set(newCore)
      yield
        val isUnresolved: Boolean = newCore.trump match
          case Trump.WizardUnresolved(c) => true
          case _                         => false

        if isUnresolved then GameState.ChoosingTrump(newCore)
        else
          GameState.Bidding(
            core = newCore,
            currentBids = Bids.empty,
            currentPlayer = firstPlayer
          )

  extension (expectedPlayer: PlayerId)
    /**
     * Validates if the action is being performed by the player whose turn it currently is.
     *
     * @param actionPlayer the ID of the player attempting to make a move.
     * @return Right(()) if the turn is valid, Left with [[GameError.NotYourTurn]] otherwise.
     */
    def validateTurnOf(actionPlayer: PlayerId): Either[GameError, Unit] =
      Either.cond(actionPlayer == expectedPlayer, (), GameError.NotYourTurn)

export RoundManager.*
