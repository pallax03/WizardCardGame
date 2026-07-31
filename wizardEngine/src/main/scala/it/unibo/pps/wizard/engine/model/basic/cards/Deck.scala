package it.unibo.pps.wizard.engine.model.basic.cards

/**
 * Represents the deck used in the Wizard Card Game.
 *
 * A standard Wizard deck is composed of 60 unique cards:
 *   - 13 Cards for every Color (4), Ranked from 1 to 13: 52 Ranked Cards
 *   - 4 Wizard card
 *   - 4 Jester card
 *
 * Each round requires a newly initialized deck to ensure players do not receive duplicate cards.
 */
opaque type Deck = List[Card]

object Deck:
  import cats.data.State

  final val TOTAL_WIZARD: Int = 4
  final val TOTAL_JESTER: Int = 4
  final val TOTAL_SIZE: Int =
    TOTAL_JESTER + TOTAL_WIZARD + (Card.Rank.values.length * Card.Color.values.length)

  /**
   * Creates a custom deck from a provided list of cards.
   *
   * @param cards the list of cards used to compose the deck. Duplicates are removed.
   * @return a custom [[Deck]].
   */
  def create(cards: List[Card]): Deck = cards.distinct

  /**
   * Creates a newly initialized and shuffled standard Wizard deck.
   *
   * @return a shuffled 60-card [[Deck]].
   */
  def create: Deck = DeckFactory.create()

  extension (d: Deck)
    def length: Int = d.length
    def cards: List[Card] = d

  /**
   * Draws a specified number of cards from the deck.
   *
   * From a Functional Programming perspective, this method uses `cats.data.State` to avoid
   * mutating the deck in place. It returns a state computation that produces both the
   * remaining deck and the drawn cards.
   *
   * @param n the number of cards to draw.
   * @note If the requested number of cards exceeds the deck's current size,
   *       all remaining cards are drawn and the new deck state becomes empty.
   *
   * @return a `State` instance representing the state transition, yielding a `List[Card]`.
   */
  def pop(n: Int): State[Deck, List[Card]] =
    State: (currentDeck: Deck) =>
      currentDeck.splitAt(n).swap

  private object DeckFactory:
    import scala.util.Random
    import Card.*
    def create(): Deck =
      val standards = for
        color <- Color.values.toList
        rank <- Rank.values.toList
      yield rank of color

      val wizards = List.fill(TOTAL_WIZARD)(wizard)
      val jesters = List.fill(TOTAL_JESTER)(jester)

      Random.shuffle(standards ++ wizards ++ jesters)
