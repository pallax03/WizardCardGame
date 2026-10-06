package io.github.pallax03.wizard.engine.model.basic.gameplay

import io.github.pallax03.wizard.engine.model.basic.PlayerId
import io.github.pallax03.wizard.engine.model.basic.cards.Card
import io.github.pallax03.wizard.engine.model.core.GameException

/**
 * Represents the cards currently played on the table during a trick.
 * It maintains the order of play as a sequence of associations between
 * [[PlayerId]] and the [[Card]] played.
 */
opaque type Table = List[(PlayerId, Card)]

object Table:
  def empty: Table = List.empty

  extension (t: Table)
    def isTrickComplete(totalPlayers: Int): Boolean = t.size == totalPlayers
    def playedCards: List[Card] = t.map(_._2)

    /**
     * Returns the [[PlayerId]] of the player who played the given card.
     *
     * @throws GameException.TableNoWinner if the card was not found on the table.
     */
    def playerOf(card: Card): PlayerId =
      t.find(_._2 == card)
        .map(_._1)
        .getOrElse(throw GameException.TableNoWinner)

    /**
     * Determines the suit that following players are required to follow in the current trick.
     *
     * According to Wizard rules:
     *   - If a [[Card.Wizard]] was played, there is no required following color.
     *   - If the trick starts with one or more [[Card.Jester]]s, the following color is set
     *     by the first [[Card.Standard]] card played after them.
     *   - If no standard cards have been played yet (e.g. only Jesters), no following color is set.
     *
     * @return `Some(Color)` if a standard card determines the lead suit, `None` otherwise.
     */
    def followingColor: Option[Card.Color] = t.playedCards match
      case cards if cards.exists(_.isWizard) => None
      case cards =>
        cards
          .dropWhile(_.isJester)
          .headOption
          .collect { case s: Card.Standard => s.color }

    infix def +(play: (PlayerId, Card)): Table = t :+ play
