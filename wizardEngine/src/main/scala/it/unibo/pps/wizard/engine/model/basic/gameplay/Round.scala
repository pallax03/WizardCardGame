package it.unibo.pps.wizard.engine.model.basic.gameplay

/** Represents the current round number of the game. */
opaque type Round = Int

object Round:
  /** Returns the starting round of a match. */
  def start: Round = 1

  def apply(value: Int): Round = value

  extension (r: Round)
    /** Returns the underlying integer value of the round. */
    def value: Int = r

    /** Returns the next consecutive round. */
    def next: Round = r + 1
