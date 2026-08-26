package io.github.pallax03.wizard.engine.model.basic

import io.github.pallax03.wizard.engine.model.basic.PlayerId
import io.github.pallax03.wizard.engine.model.basic.bidding.Bid
import io.github.pallax03.wizard.engine.model.basic.gameplay.Round

/** Represents the score accumulated by a player. */
type Score = Int

/**
 * Represents the history of scores and bids for all players across game rounds.
 * Maps each [[PlayerId]] to another map of [[Round]] to their corresponding [[Score]] and [[Bid]].
 */
opaque type Scoreboard = Map[PlayerId, Map[Round, (Score, Bid)]]
object Scoreboard:
  def empty: Scoreboard = Map.empty
  def apply(map: Map[PlayerId, Map[Round, (Score, Bid)]]): Scoreboard = map

  extension (sb: Scoreboard)
    /**
     * Returns the round-by-round history for a specific player.
     *
     * @param pId the target player ID.
     * @return a map of rounds to the player's score and bid.
     */
    def apply(pId: PlayerId): Map[Round, (Score, Bid)] = sb.getOrElse(pId, Map.empty)

    /**
     * Records a score and a bid for a player in a specific round.
     *
     * @param pId      the player ID.
     * @param round  the current round.
     * @param points the score obtained in the round.
     * @param bid    the bid placed for the round.
     * @return the updated [[Scoreboard]].
     */
    def addScore(pId: PlayerId, round: Round, points: Score, bid: Bid): Scoreboard =
      sb.updated(pId, sb.getOrElse(pId, Map.empty).updated(round, (points, bid)))

    /**
     * Returns the score and bid of a player for a specific round.
     *
     * @param r   the target round.
     * @param pId the player ID.
     * @return a tuple of [[Score]] and [[Bid]], defaulting to (0, 0) if not found.
     */
    def getStatsForRound(r: Round, pId: PlayerId): (Score, Bid) =
      sb.getOrElse(pId, Map.empty).getOrElse(r, (0, 0))

    def toMap: Map[PlayerId, Map[Round, (Score, Bid)]] = sb
