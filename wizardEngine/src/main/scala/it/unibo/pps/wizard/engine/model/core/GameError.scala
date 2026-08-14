package it.unibo.pps.wizard.engine.model.core

import it.unibo.pps.wizard.engine.model.basic.PlayerId
import it.unibo.pps.wizard.engine.model.basic.cards.Card

/**
 * Reasons why a card move is considered illegal during a trick.
 *
 * @param legitCards a list of cards that would have been valid in this situation,
 *                   useful for the UI to provide feedback to the player.
 */
enum CardNotAllowedReasons(val legitCards: List[Card]):

  /** The card played is not present in player's hand. */
  case CardNotInHand(cards: List[Card]) extends CardNotAllowedReasons(cards)

  /**
   * The player played a standard card of a different color,
   * despite having at least one card of the required following color.
   */
  case MustFollowColor(requiredColor: Card.Color, cards: List[Card])
      extends CardNotAllowedReasons(cards)

enum InconsistentStateReasons:

  /** Occurs when the trick table is empty or invalid at the moment of evaluation. */
  case TableNoWinner

  /** Occurs when the system expects a hand for a player that does not exist in the state. */
  case HandNotFoundFor(playerId: PlayerId)

  /** Occurs when the serialized state (e.g., from Redis) cannot be decoded properly. */
  case CorruptedState(message: String)

/** Represents all possible errors that can occur during the execution of a [[GameAction]]. */
enum GameError:

  /** The action was performed by a player who is not the current turn holder. */
  case NotYourTurn

  /** The bid placed does not respect the game rules. (lastBid must be sumOfAllBids != round ) */
  case InvalidBid

  /** The action is not permitted in the current [[GameState]]. */
  case InvalidAction

  /** The card played is not allowed based on current table rules. */
  case CardNotAllowed(reason: CardNotAllowedReasons)

  /**
   * Indicates an internal system error where the game state became corrupted or inconsistent.
   * This suggests a logic error in the engine's transition handling.
   */
  case InconsistentState(reason: InconsistentStateReasons)

/** Exception wrapper for GameError, useful to fail Futures with specific domain errors */
case class GameException(error: GameError) extends Exception(error.toString)
