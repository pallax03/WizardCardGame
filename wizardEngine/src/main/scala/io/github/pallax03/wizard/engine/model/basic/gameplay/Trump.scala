package io.github.pallax03.wizard.engine.model.basic.gameplay

import io.github.pallax03.wizard.engine.model.basic.cards.Card
import io.github.pallax03.wizard.engine.model.core.GameError

/**
 * Represents the trump card (briscola) for a round and its resolution state.
 *
 * In Wizard, the trump card determines the trump suit: any [[Card.Standard]]
 * matching the `effectiveColor` overrides cards of different colors within a trick.
 *
 * @note Use [[Trump.apply(...)]] to automatically lift any [[Card]] into its
 *       corresponding [[Trump]] state.
 */
enum Trump:
  /** No trump card is present (during last round). */
  case Absent

  /** A Jester determines No Trump Card */
  case Jester(c: Card.Jester)

  /** A Standard card determines the trump color. */
  case Standard(c: Card.Standard)

  /** A Wizard was drawn as trump, the player need to choose the color. */
  case WizardUnresolved(c: Card.Wizard)

  /** A Wizard was drawn as trump and has been resolved to a specific color chosen by the player. */
  case WizardResolved(c: Card.Wizard, color: Card.Color)

  /**
   * Returns the effective's trump color associated with the trump card.
   *
   * @return [[Some(Color)]] if Standard or ResolvedWizard, [[None]] if else.
   */
  def effectiveColor: Option[Card.Color] = this match
    case Standard(c)              => Some(c.color)
    case WizardResolved(_, color) => Some(color)
    case _                        => None

  /** Retrieves the card drawn as a Trump, if any. */
  def card: Option[Card] = this match
    case Absent               => None
    case Jester(c)            => Some(c)
    case Standard(c)          => Some(c)
    case WizardUnresolved(c)  => Some(c)
    case WizardResolved(c, _) => Some(c)

object Trump:
  /**
   * Factory method to lift a [[Card]] into a [[Trump]].
   *
   * @param c The card determining the trump.
   */
  def apply(c: Card): Trump = c match
    case j: Card.Jester   => Trump.Jester(j)
    case w: Card.Wizard   => Trump.WizardUnresolved(w)
    case s: Card.Standard => Trump.Standard(s)

  extension (t: Trump)
    /**
     * Attempts to resolve an unresolved Wizard trump into a specific color.
     *
     * @param color the color chosen by the player.
     * @return [[Right]] containing the resolved Trump, or [[Left(InvalidAction)]]
     *         if the Trump is not a Wizard or already resolved.
     */
    infix def resolveWizard(color: Card.Color): Either[GameError, Trump] = t match
      case Trump.WizardUnresolved(c) => Right(Trump.WizardResolved(c, color))
      case _                         => Left(GameError.InvalidAction(None))
