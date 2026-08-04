package it.unibo.pps.wizard.engine.model.basic.scoreboard

import it.unibo.pps.wizard.engine.model.basic.PlayerId
import it.unibo.pps.wizard.engine.model.basic.Players
import it.unibo.pps.wizard.engine.model.basic.bidding.Bid
import it.unibo.pps.wizard.engine.model.basic.gameplay.Round
import it.unibo.pps.wizard.engine.model.basic.scoreboard.RoundRow

/** Represents the score accumulated by a player. */
type Score = Int
object Score:
  def apply(points: Int): Score = points

/**
 * Represents the history of scores and bids for all players across game rounds.
 * Maps each [[PlayerId]] to another map of [[Round]] to their corresponding [[Score]] and [[Bid]].
 */
opaque type Scoreboard = Map[PlayerId, Map[Round, (Score, Bid)]]
object Scoreboard:
  def empty: Scoreboard = Map.empty

  extension (sb: Scoreboard)
    /**
     * Returns the round-by-round history for a specific player.
     *
     * @param p the target player ID.
     * @return a map of rounds to the player's score and bid.
     */
    def apply(p: PlayerId): Map[Round, (Score, Bid)] = sb.getOrElse(p, Map.empty)

    /**
     * Records a score and a bid for a player in a specific round.
     *
     * @param p      the player ID.
     * @param round  the current round.
     * @param points the score obtained in the round.
     * @param bid    the bid placed for the round.
     * @return the updated [[Scoreboard]].
     */
    def addScore(p: PlayerId, round: Round, points: Score, bid: Bid): Scoreboard =
      sb.updated(p, sb.getOrElse(p, Map.empty).updated(round, (points, bid)))

    /**
     * Returns the score and bid of a player for a specific round.
     *
     * @param r   the target round.
     * @param pId the player ID.
     * @return a tuple of [[Score]] and [[Bid]], defaulting to (0, 0) if not found.
     */
    def getStatsForRound(r: Round, pId: PlayerId): (Score, Bid) =
      sb.getOrElse(pId, Map.empty).getOrElse(r, (Score(0), 0))

    private def allPlayedRounds: List[Round] = sb.values.flatMap(_.keys).toSet.toList.sorted

    /**
     * Converts the current scoreboard data into a list of round rows.
     *
     * @param players the list of players.
     * @return a list of [[RoundRow]]s containing stats for all played rounds.
     */
    def toRoundRows(players: Players): List[RoundRow] =
      sb.allPlayedRounds.map { round =>
        RoundRow(round, RoundRow.getStatsForAllPlayers(round, players, sb))
      }

given Ordering[Round] with
  def compare(x: Round, y: Round): Int = x.value.compare(y.value)
