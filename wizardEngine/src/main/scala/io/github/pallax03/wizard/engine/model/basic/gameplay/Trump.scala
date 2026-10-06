package io.github.pallax03.wizard.engine.model.basic.gameplay

import io.github.pallax03.wizard.engine.model.basic.cards.Card
import io.github.pallax03.wizard.engine.model.core.GameActionError

/**
 * Represents the trump card (briscola) for a round and its resolution state.
 *
 * In the Wizard Card Game, after dealing hands, the next card from the deck is turned face-up
 * to determine the trump suit for that round:
 *   - A [[Card.Standard]] card sets its suit as the trump suit.
 *   - A [[Card.Wizard]] requires the dealer to choose any of the 4 colors before bidding begins.
 *   - A [[Card.Jester]] means there is no trump suit for the round.
 *   - In the final round, no card remains in the deck, resulting in [[Trump.Absent]] (no trump suit).
 *
 * @note Use [[Trump.apply]] to automatically lift any drawn [[Card]] into its initial [[Trump]] state.
 */
enum Trump:
  case Absent
  case Jester(c: Card.Jester)
  case Standard(c: Card.Standard)
  case WizardUnresolved(c: Card.Wizard)
  case WizardResolved(c: Card.Wizard, color: Card.Color)

  /**
   * Returns the effective trump color for this round, if one is active.
   *
   * @return `Some(color)` if a standard card or resolved Wizard determines the trump suit, `None` otherwise.
   */
  def effectiveColor: Option[Card.Color] = this match
    case Standard(c)              => Some(c.color)
    case WizardResolved(_, color) => Some(color)
    case _                        => None

  def card: Option[Card] = this match
    case Absent               => None
    case Jester(c)            => Some(c)
    case Standard(c)          => Some(c)
    case WizardUnresolved(c)  => Some(c)
    case WizardResolved(c, _) => Some(c)

object Trump:
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
    infix def resolveWizard(color: Card.Color): Either[GameActionError, Trump] = t match
      case Trump.WizardUnresolved(c) => Right(Trump.WizardResolved(c, color))
      case _                         => Left(GameActionError.InvalidAction(None))
