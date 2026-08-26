package io.github.pallax03.wizard.engine.adapters.prolog

import io.github.pallax03.wizard.engine.model.basic.cards._
import io.github.pallax03.wizard.engine.model.basic.gameplay.Trump

/**
 * Provides utility methods to map Scala game domain models into Prolog terms.
 *
 * This object is responsible for the serialization of [[Card]], [[Color]], and [[Trump]]
 * types into strings compatible with the Prolog syntax expected by [[WizardPrologEngine]].
 */
object WizardTermMapper:

  /**
   * Constant used to represent an empty or non-existent value in Prolog terms.
   * Necessary for the wizard theory.
   */
  final val NO_VALUE: String = "none"

  /** Serializes a list of cards into a Prolog list string representation. */
  def cardsTerm(cards: List[Card]): String = cards.map(cardTerm).mkString("[", ",", "]")

  /** Serializes an optional card, mapping [[None]] to [[NO_VALUE]]. */
  def cardTerm(card: Option[Card]): String = card.map(cardTerm).getOrElse(NO_VALUE)

  /**
   * Serializes a single [[Card]] into a Prolog term.
   *
   * Special cards are mapped to their simple name (e.g., 'wizard', 'jester'),
   * while standard cards are mapped to the functor 'card(rank, color)'.
   */
  def cardTerm(card: Card): String = card match
    case card: SpecialCard          => card.getClass.getSimpleName.toLowerCase
    case Card.Standard(color, rank) => s"card(${rank.value},${colorTerm(color)})"

  /** Extracts the trump color term from the current [[Trump]] state. */
  def trumpColorTerm(trump: Trump): String = colorTerm(trump.effectiveColor)

  /** Serializes a [[Card.Color]] into a lowercase Prolog-compatible string. */
  def colorTerm(color: Card.Color): String = color.toString.toLowerCase

  /** Serializes an optional color, mapping [[None]] to [[NO_VALUE]]. */
  def colorTerm(color: Option[Card.Color]): String = color.map(colorTerm).getOrElse(NO_VALUE)
