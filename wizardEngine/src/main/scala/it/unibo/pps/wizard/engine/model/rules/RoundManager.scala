package it.unibo.pps.wizard.engine.model.rules

import cats.syntax.traverse.toTraverseOps
import cats.data.State
import it.unibo.pps.wizard.engine.model.basic.PlayerId
import it.unibo.pps.wizard.engine.model.basic.bidding.Bids
import it.unibo.pps.wizard.engine.model.basic.cards.*
import it.unibo.pps.wizard.engine.model.basic.gameplay.*
import it.unibo.pps.wizard.engine.model.core.CoreState
import it.unibo.pps.wizard.engine.model.core.GameError
import it.unibo.pps.wizard.engine.model.core.GameState

/** Manages game round lifecycle operations, player turns, card dealing, and state initialization. */
object RoundManager:

  extension (playersIds: List[PlayerId])
    /**
     * Determines the next player in the turn order following the current player.
     *
     * @param current the ID of the current player.
     * @return Right containing the next [[PlayerId]], or Left with a [[GameError]] if the player is not found.
     */
    def nextAfter(current: PlayerId): Either[GameError, PlayerId] = playersIds.indexWhere(_ == current) match
      case id if id >= 0 => Right(playersIds((id + 1) % playersIds.size))
      case _ => Left(GameError.NotYourTurn)

  extension (round: Round)
    /**
     * Determines the first player of the given round based on a shifting rotation.
     *
     * @param playersIds the list of players Ids.
     * @return the [[PlayerId]] of the dealer's successor who starts the round.
     */
    def firstPlayer(playersIds: List[PlayerId]): PlayerId =
      playersIds((round - 1) % playersIds.size)

    /**
     * Checks if this round is the final round of the game based on the player count.
     *
     * @param playersIds the list of players Ids.
     * @return true if all cards in the deck are distributed evenly, false otherwise.
     */
    def isLastRound(playersIds: List[PlayerId]): Boolean =
      round == (Deck.TOTAL_SIZE / playersIds.size)

    /**
     * State action that deals cards to players and reveals the trump card.
     *
     * @param playersIds the list of players Ids.
     * @return a state transition resulting in a tuple of [[Hands]] and a [[Trump]].
     */
    def deal(playersIds: List[PlayerId]): State[Deck, (Hands, Trump)] =
      val cardsPerPlayer = round
      for
        handsList <- playersIds.traverse(p => Deck.pop(cardsPerPlayer).map(p.holds))
        trump <- Deck.pop(1).map(_.headOption)
      yield (Hands(handsList.toMap), trump.asTrump)

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

        (hands, trump) = round.deal(core.playersIds).runA(deck).value

        newCore = core.copy(
          hands = hands,
          trump = trump
        )

        _ <- State.set(newCore)
      yield
        trump match
          case _: Trump.WizardUnresolved => GameState.ChoosingTrump(newCore)
          case _                         => GameState.Bidding(core = newCore, currentBids = Bids.empty, currentPlayer = round.firstPlayer(core.playersIds))

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
