package io.github.pallax03.wizard.engine.model.rules

import cats.data.State
import cats.syntax.traverse.toTraverseOps

import io.github.pallax03.wizard.engine.model.basic.PlayerId
import io.github.pallax03.wizard.engine.model.basic.bidding.Bids
import io.github.pallax03.wizard.engine.model.basic.cards.*
import io.github.pallax03.wizard.engine.model.basic.gameplay.*
import io.github.pallax03.wizard.engine.model.core.state.{GameState, ServerCoreState}
import io.github.pallax03.wizard.engine.model.core.{GameActionError, GameException}

/** Manages game round lifecycle operations, player turns, card dealing, and state initialization. */
object RoundManager:

  extension (playersIds: List[PlayerId])
    /**
     * Returns the next player in clockwise cyclic turn order.
     *
     * @throws GameException.PlayerNotFound if the player is not in the list.
     */
    def nextAfter(current: PlayerId): PlayerId =
      playersIds.indexWhere(_ == current) match
        case id if id >= 0 => playersIds((id + 1) % playersIds.size)
        case _             => throw GameException.PlayerNotFound(current)

  extension (round: Round)
    /**
     * Determines the starting player (leader) of the given round based on clockwise rotation.
     *
     * In Wizard, the dealer role rotates each round. The round index directly offsets
     * the starting player: Round 1 starts with player 0, Round 2 with player 1, etc.
     *
     * @param playersIds the ordered list of player IDs.
     * @return the [[PlayerId]] who starts the bidding and the first trick of the round.
     */
    def firstPlayer(playersIds: List[PlayerId]): PlayerId =
      playersIds((round - 1) % playersIds.size)

    /**
     * Checks whether this round is the final round of the match.
     *
     * Wizard matches end when the entire 60-card deck is distributed as evenly as possible:
     * `Deck.TOTAL_SIZE / playerCount` (e.g. 20 rounds for 3 players, 15 for 4, 12 for 5, 10 for 6).
     *
     * @param playersIds the list of participating player IDs.
     * @return `true` if this is the final round of the game, `false` otherwise.
     */
    def isLastRound(playersIds: List[PlayerId]): Boolean =
      round == (Deck.TOTAL_SIZE / playersIds.size)

    /**
     * Purely functional state action that deals hands to all players and cuts the trump card from the deck.
     *
     * Uses `cats.data.State[Deck, _]` to thread the remaining cards immutably.
     *
     * @param playersIds the list of players to deal cards to.
     * @return a state transition computing a tuple of dealt [[Hands]] and the revealed [[Trump]].
     */
    def deal(playersIds: List[PlayerId]): State[Deck, (Hands, Trump)] =
      val cardsPerPlayer = round
      for
        handsList <- playersIds.traverse(p => Deck.pop(cardsPerPlayer).map(p.holds))
        trump <- Deck.pop(1).map(_.headOption)
      yield (Hands(handsList.toMap), trump.asTrump)

    /**
     * Purely functional state action that initializes a new round within the [[ServerCoreState]].
     *
     * Deals cards from the given deck, updates server hands and trump, and decides the next
     * phase: [[GameState.ChoosingTrump]] if a Wizard was turned up as trump, or [[GameState.Bidding]] otherwise.
     *
     * @param deck the shuffled [[Deck]] to draw from for this round.
     * @return a state transition yielding the initial [[GameState]] for the round.
     */
    def initialize(deck: Deck): State[ServerCoreState, GameState[ServerCoreState]] =
      for
        core <- State.get[ServerCoreState]
        (dealtHands, roundTrump) = round.deal(core.playersIds).runA(deck).value

        newCore = core.copy(
          hands = dealtHands,
          trump = roundTrump,
          dealerId = core.round.firstPlayer(core.playersIds)
        )

        _ <- State.set(newCore)
      yield roundTrump match
        case _: Trump.WizardUnresolved => GameState.ChoosingTrump(newCore)
        case _ =>
          GameState.Bidding(
            core = newCore,
            bids = Bids.empty,
            playerTurn = newCore.round.firstPlayer(newCore.playersIds)
          )

  extension (expectedPlayer: PlayerId)
    /** Validates whether an incoming action is performed by the expected active player. */
    def validateTurnOf(actionPlayer: PlayerId): Either[GameActionError, Unit] =
      Either.cond(actionPlayer == expectedPlayer, (), GameActionError.NotYourTurn(expectedPlayer))

export RoundManager.*
