package io.github.pallax03.wizard.engine.model.basic.gameplay

/** Represents the current round number of the game. */
type Round = Int

object Round:
  /** Returns the starting round of a match. */
  def start: Round = 1

  extension (r: Round)
    /** Returns the next consecutive round. */
    def next: Round = r + 1

export Round._
