package io.github.pallax03.wizard.engine.model.basic.cards

import java.util.concurrent.atomic.AtomicInteger

import io.github.pallax03.wizard.engine.model.basic.gameplay.Trump

/**
 * Represents a card in the Wizard game.
 *
 * In the Wizard deck of 60 cards, a card is either a [[Card.Standard]] suited card
 * with a color and rank (52 cards total), or a [[SpecialCard]] ([[Card.Wizard]] or [[Card.Jester]]).
 *
 * @see [[Deck]] for deck composition.
 */
sealed trait Card

/**
 * A special card in the Wizard game (Wizard or Jester).
 *
 * Each special card carries a unique numeric ID (0 to 3) to distinguish between the four
 * physical copies of the same special card type in the deck.
 */
sealed trait SpecialCard extends Card:
  /** Unique instance identifier among duplicate special cards in the deck. */
  def id: Int

object Card:

  enum Color:
    case Blue, Green, Red, Yellow
  export Color.*

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

  final case class Standard(color: Color, rank: Rank) extends Card
  final case class Wizard(id: Int) extends SpecialCard
  final case class Jester(id: Int) extends SpecialCard

  private val specialIdGenWizard = new AtomicInteger(0)
  def wizard: Wizard = Wizard(specialIdGenWizard.incrementAndGet() % Deck.TOTAL_WIZARD)

  private val specialIdGenJester = new AtomicInteger(0)
  def jester: Jester = Jester(specialIdGenJester.incrementAndGet() % Deck.TOTAL_JESTER)

  extension (rank: Rank) infix def of(color: Color): Card = Standard(color, rank)

  extension (optCard: Option[Card])
    def asTrump: Trump = optCard match
      case Some(card) => Trump(card)
      case None       => Trump.Absent

  extension (c: Card)
    def isWizard: Boolean = c match
      case _: Wizard => true
      case _         => false

    def isJester: Boolean = c match
      case _: Jester => true
      case _         => false

export Card.{wizard, jester}
