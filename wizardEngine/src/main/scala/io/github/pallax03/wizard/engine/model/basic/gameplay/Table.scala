package io.github.pallax03.wizard.engine.model.basic.gameplay

import io.github.pallax03.wizard.engine.model.basic.PlayerId
import io.github.pallax03.wizard.engine.model.basic.cards.Card
import io.github.pallax03.wizard.engine.model.core.{GameException, InconsistentState}

/**
 * Represents the cards currently played on the table during a trick.
 * It maintains the order of play as a sequence of associations between
 * [[PlayerId]] and the [[Card]] played.
 */
opaque type Table = List[(PlayerId, Card)]

object Table:
  def empty: Table = List.empty

  extension (t: Table)
    /** Checks if all players have played their card for the current trick. */
    def isTrickComplete(totalPlayers: Int): Boolean = t.size == totalPlayers

    def playedCards: List[Card] = t.map(_._2)

    /**
     * Returns the [[PlayerId]] of the player who played the given card.
     * @throws GameException if the card is not found on the table.
     */
    def playerOf(card: Card): PlayerId =
      t.find(_._2 == card)
        .map(_._1)
        .getOrElse(throw GameException(InconsistentState.TableNoWinner))

    /**
     * Determines the color that players must follow in the current trick.
     *
     * @return Some(Color) if a standard card determines the color,
     *         None if the trick contains a Wizard or only Jesters have been played.
     */
    def followingColor: Option[Card.Color] = t.playedCards match
      case cards if cards.exists(_.isWizard) => None
      case cards =>
        cards
          .dropWhile(_.isJester)
          .headOption
          .collect { case s: Card.Standard => s.color }

    /** Adds a card played by a player to the table. */
    infix def +(play: (PlayerId, Card)): Table = t :+ play
