package it.unibo.pps.wizard.engine.model.basic.bidding

import it.unibo.pps.wizard.engine.model.basic.PlayerId
import it.unibo.pps.wizard.engine.model.basic.Players

/** Represents the number of tricks won by a player. */
type Trick = Int

object Trick:
  def apply(value: Int): Trick = value

/** Represents the count of won tricks for each player in the current round. */
opaque type Tricks = Map[PlayerId, Trick]

object Tricks:
  /**
   * Initializes the trick count to 0 for all active players.
   *
   * @param players the list of players.
   * @return a [[Tricks]] map with all player scores set to 0.
   */
  def initialize(players: Players): Tricks =
    players.toList.map(_.id -> 0).toMap
  def empty: Tricks = Map.empty

  extension (t: Tricks)
    /**
     * Returns the number of tricks won by a specific player, defaulting to 0 if not found.
     *
     * @param p the player ID.
     * @return the number of tricks won.
     */
    def apply(p: PlayerId): Trick = t.getOrElse(p, 0)

    /**
     * Increments the trick count by 1 for the player who won the current trick.
     *
     * @param p the player ID of the trick winner.
     * @return the updated [[Tricks]] collection.
     */
    def addTrickTo(p: PlayerId): Tricks = t.updated(p, t.getOrElse(p, 0) + 1)
