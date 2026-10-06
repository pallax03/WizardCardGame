package io.github.pallax03.wizard.engine.adapters.prolog

import io.github.pallax03.wizard.engine.model.basic.cards.*
import io.github.pallax03.wizard.engine.model.basic.gameplay.Trump

/**
 * Translates Scala card domain entities into Prolog term representations for the 2P-Kt engine.
 *
 * Serialization format expected by `wizard_strategy.pl`:
 *  - Standard cards: compound term `card(Rank, Color)` (e.g. `card(13,red)`).
 *  - Special cards: atomic terms `'wizard'` and `'jester'`.
 *  - Lists: Prolog list syntax `[Term1, Term2, ...]`.
 *  - Absent values (e.g. no trump color): atomic string [[NO_VALUE]] (`"none"`).
 */
object WizardTermMapper:

  /** Constant representing an empty or undefined term in the Wizard Prolog theory. */
  final val NO_VALUE: String = "none"

  def cardsTerm(cards: List[Card]): String = cards.map(cardTerm).mkString("[", ",", "]")

  def cardTerm(card: Option[Card]): String = card.map(cardTerm).getOrElse(NO_VALUE)

  /** Maps [[Card.Standard]] to `card(Rank, Color)` and [[SpecialCard]] to `'wizard'` / `'jester'`. */
  def cardTerm(card: Card): String = card match
    case card: SpecialCard          => card.getClass.getSimpleName.toLowerCase
    case Card.Standard(color, rank) => s"card(${rank.value},${colorTerm(color)})"

  def trumpColorTerm(trump: Trump): String = colorTerm(trump.effectiveColor)

  def colorTerm(color: Card.Color): String = color.toString.toLowerCase

  def colorTerm(color: Option[Card.Color]): String = color.map(colorTerm).getOrElse(NO_VALUE)
