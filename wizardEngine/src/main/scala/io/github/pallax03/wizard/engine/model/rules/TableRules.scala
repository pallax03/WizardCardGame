package io.github.pallax03.wizard.engine.model.rules

import io.github.pallax03.wizard.engine.model.basic.cards._
import io.github.pallax03.wizard.engine.model.basic.gameplay._
import io.github.pallax03.wizard.engine.model.core.CardNotAllowedReasons._
import io.github.pallax03.wizard.engine.model.core.GameError

/** Defines the rules for card validation and trick evaluation. */
object TableRules:

  extension (h: Hand)
    private def hasColor(color: Card.Color): Boolean = h.toList.exists:
      case Card.Standard(c, _) => c == color
      case _                   => false

    /** Determines which cards are legally playable based on the current table state. */
    def legalCards(table: Table): List[Card] = h.toList.filter:
      case _: SpecialCard      => true
      case Card.Standard(c, _) => table.followingColor.fold(true)(fc => c == fc || !h.hasColor(fc))

  extension (cardPlayed: Card)
    /**
     * Validates if a card can be played according to the current game rules.
     *
     * @param table the current state of the table to check the following color.
     * @param hand the hand of the player attempting the move.
     * @return Right if the move is valid, else: Left([[GameError]]) if the move violates game rules:
     *         - [[CardNotInHand]]: if the card is not present in the player's hand.
     *         - [[MustFollowColor]]: if a color must be followed but a different
     *           standard card is played.
     */
    def validateAgainst(table: Table, hand: Hand): Either[GameError, Unit] =
      if !hand.contains(cardPlayed) then
        Left(GameError.CardNotAllowed(CardNotInHand(hand.legalCards(table))))
      else
        cardPlayed match
          case _: SpecialCard => Right(())
          case Card.Standard(playedColor, _) =>
            table.followingColor match
              case Some(followingColor)
                  if playedColor != followingColor && hand.hasColor(followingColor) =>
                Left(
                  GameError.CardNotAllowed(
                    MustFollowColor(followingColor, hand.legalCards(table))
                  )
                )
              case _ => Right(())

  extension (table: Table)
    /**
     * Evaluates the winner of the trick based on the trump card.
     *
     * Trick logic:
     * 1. The first Wizard played wins the trick.
     * 2. Otherwise, the highest Trump card wins.
     * 3. Otherwise, the highest card of the following color wins.
     * 4. Otherwise, the first card played wins (Jesters only).
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
