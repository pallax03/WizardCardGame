package it.unibo.pps.wizard.engine.model.basic.cards

import it.unibo.pps.wizard.engine.model.basic.PlayerId
import it.unibo.pps.wizard.engine.model.core.GameException
import it.unibo.pps.wizard.engine.model.core.InconsistentState.CorruptedHand

/**
 * Represents the set of cards currently held by a single player.
 * Implemented as an opaque type over List[Card] to provide domain-specific
 * operations while hiding standard collection methods.
 */
opaque type Hand = List[Card]

object Hand:
  def empty: Hand = List.empty
  def apply(cards: List[Card]): Hand = cards

  /** Removes a specific card from the hand, if present. */
  def without(hand: Hand, card: Card): Hand = hand.filterNot(_ == card)

  extension (h: Hand)
    def isEmpty: Boolean = h.isEmpty
    def contains(card: Card): Boolean = h.contains(card)
    def toList: List[Card] = h



/**
 * Represents the state of all players' hands in the game.
 * Maps each PlayerId to their respective Hand.
 */
opaque type Hands = Map[PlayerId, Hand]

object Hands:
  def empty: Hands = Map.empty
  def apply(hands: Map[PlayerId, Hand]): Hands = hands

  extension (hands: Hands)
    /**
     * Retrieves the Hand of a specific player.
     * 
     * @param player The ID of the player.
     * @return The player's Hand.
     * @throws GameException if the player is not found, indicating a corrupted system state.
     */
    def getHand(player: PlayerId): Hand =
      hands.getOrElse(player, throw GameException(CorruptedHand(player)))

    /**
     * Removes a specific card from a player's hand.
     * 
     * @param player The ID of the player.
     * @param card The card to remove.
     * @return A new Hands instance with the card removed.
     * @throws GameException if the player is not found, indicating a corrupted system state.
     */
    def remove(player: PlayerId, card: Card): Hands =
      val hand = hands.getHand(player)
      hands.updated(player, Hand.without(hand, card))

    def areEmpty: Boolean = hands.values.forall(_.isEmpty)
    private[wizard] def toMap: Map[PlayerId, Hand] = hands
