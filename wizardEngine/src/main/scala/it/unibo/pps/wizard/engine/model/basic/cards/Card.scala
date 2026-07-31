package it.unibo.pps.wizard.engine.model.basic.cards

import it.unibo.pps.wizard.engine.model.basic.gameplay.Trump

import java.util.concurrent.atomic.AtomicInteger

/**
 * Represents a card in the Wizard game.
 *
 * A card can be either a standard card with a color and rank, or a special card (Wizard or Jester).
 */
sealed trait Card

/**
 * A special card in the Wizard game (Wizard or Jester).
 *
 * Each special card has a unique ID to distinguish between multiple instances of the same type.
 */
sealed trait SpecialCard extends Card:
  def id: Int

object Card:

  /** Color of a standard card. */
  enum Color:
    case Blue, Green, Red, Yellow
  export Color.*

  /** Rank in the standard color cards. Values 1..13 (1 is low, 13 is high). */
  enum Rank(val value: Int):
    case One extends Rank(1)
    case Two extends Rank(2)
    case Three extends Rank(3)
    case Four extends Rank(4)
    case Five extends Rank(5)
    case Six extends Rank(6)
    case Seven extends Rank(7)
    case Eight extends Rank(8)
    case Nine extends Rank(9)
    case Ten extends Rank(10)
    case Eleven extends Rank(11)
    case Twelve extends Rank(12)
    case Thirteen extends Rank(13)
  export Rank.*

  /** A standard card with a color and rank. */
  final case class Standard(color: Color, rank: Rank) extends Card

  /** A special Wizard card with a unique ID. */
  final case class Wizard(id: Int) extends SpecialCard

  /** A special Jester card with a unique ID. */
  final case class Jester(id: Int) extends SpecialCard

  private val specialIdGenWizard = new AtomicInteger(0)
  def wizard: Wizard = Wizard(specialIdGenWizard.incrementAndGet() % Deck.TOTAL_WIZARD)
  private val specialIdGenJester = new AtomicInteger(0)
  def jester: Jester = Jester(specialIdGenJester.incrementAndGet() % Deck.TOTAL_JESTER)

  extension (rank: Rank) infix def of(color: Color): Card = Standard(color, rank)

  extension (c: Card)
    infix def -(other: Card): List[Card] = List(c, other)
    def isWizard: Boolean = c match
      case _: Wizard => true
      case _         => false
    def isJester: Boolean = c match
      case _: Jester => true
      case _         => false

  extension (optCard: Option[Card])
    def asTrump: Trump = optCard match
      case Some(card) => Trump(card)
      case None       => Trump.Absent

  extension (cards: List[Card]) infix def -(other: Card): List[Card] = cards :+ other
