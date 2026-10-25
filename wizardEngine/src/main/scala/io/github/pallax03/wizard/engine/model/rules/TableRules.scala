package io.github.pallax03.wizard.engine.model.rules

import io.github.pallax03.wizard.engine.model.basic.cards.*
import io.github.pallax03.wizard.engine.model.basic.gameplay.*
import io.github.pallax03.wizard.engine.model.core.CardNotAllowedReasons.*
import io.github.pallax03.wizard.engine.model.core.GameActionError

/**
 * Defines the core rules for card play validity and trick evaluation in Wizard.
 *
 * Rule highlights:
 *   - **Following Suit**: Players must play a card matching the [[Table.followingColor]] if they hold one in hand.
 *   - **Special Cards Exemption**: A [[SpecialCard]] ([[Card.Wizard]] or [[Card.Jester]]) can always be played legally,
 *     regardless of the lead suit and regardless of what cards the player holds in hand.
 *   - **Trick Resolution**:
 *     1. The first [[Card.Wizard]] played wins the trick.
 *     2. Otherwise, the highest [[Card.Standard]] of the trump suit wins.
 *     3. Otherwise, the highest [[Card.Standard]] of the lead suit wins.
 *     4. Otherwise (e.g. If only Jesters were played), the first card played wins.
 */
object TableRules:

  extension (h: Hand)
    private def hasColor(color: Card.Color): Boolean = h.toList.exists:
      case Card.Standard(c, _) => c == color
      case _                   => false

    /** Returns all cards in hand that are legally playable given the current table state. */
    def legalCards(table: Table): List[Card] = h.toList.filter:
      case _: SpecialCard      => true
      case Card.Standard(c, _) => table.followingColor.fold(true)(fc => c == fc || !h.hasColor(fc))

  extension (cardPlayed: Card)
    /**
     * Validates whether a card can be legally played from hand onto the table.
     *
     * Fails with [[GameActionError.CardNotAllowed]] if the card is not in hand or violates the following color.
     */
    def validateAgainst(table: Table, hand: Hand): Either[GameActionError, Unit] =
      if !hand.contains(cardPlayed) then
        Left(GameActionError.CardNotAllowed(CardNotInHand(hand.legalCards(table))))
      else
        cardPlayed match
          case _: SpecialCard => Right(())
          case Card.Standard(playedColor, _) =>
            table.followingColor match
              case Some(followingColor)
                  if playedColor != followingColor && hand.hasColor(followingColor) =>
                Left(
                  GameActionError.CardNotAllowed(
                    MustFollowColor(followingColor, hand.legalCards(table))
                  )
                )
              case _ => Right(())

  extension (table: Table)
    /**
     * Evaluates the winning card of the current trick based on the active trump.
     *
     * Trick resolution order:
     *   1. The first [[Card.Wizard]] played wins the trick.
     *   2. Otherwise, the highest [[Card.Standard]] of the trump suit wins.
     *   3. Otherwise, the highest [[Card.Standard]] of the lead/following suit wins.
     *   4. Otherwise (e.g. only Jesters were played), the first card played wins.
     *
     * @param trump the active [[Trump]] state of the round.
     * @return `Some(winningCard)` representing the winning card, or `None` if the table is empty.
     */
    def evaluateTrick(trump: Trump): Option[Card] =
      val cards = table.playedCards
      val trumpColor = trump.effectiveColor
      val followingColor = table.followingColor

      def highestOf(targetColor: Option[Card.Color]): Option[Card] =
        cards
          .collect { case c @ Card.Standard(color, rank) if targetColor.contains(color) => c }
          .maxByOption(_.rank.value)

      cards
        .find(_.isWizard)
        .orElse(highestOf(trumpColor))
        .orElse(highestOf(followingColor))
        .orElse(cards.headOption)

export TableRules.*
