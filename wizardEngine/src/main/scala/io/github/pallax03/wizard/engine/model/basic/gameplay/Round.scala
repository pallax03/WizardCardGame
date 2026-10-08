package io.github.pallax03.wizard.engine.model.basic.gameplay

/**
 * Represents the round index (1-based) of an active Wizard match.
 *
 * In the Wizard Card Game, the round number directly determines the number of cards
 * dealt to each player (e.g. Round 1 deals 1 card each, Round 5 deals 5 cards each).
 *
 * @see [[io.github.pallax03.wizard.engine.model.rules.RoundManager]] for round lifecycle and transitions.
 */
type Round = Int

object Round:
  def start: Round = 1

  extension (r: Round) def next: Round = r + 1

export Round._
