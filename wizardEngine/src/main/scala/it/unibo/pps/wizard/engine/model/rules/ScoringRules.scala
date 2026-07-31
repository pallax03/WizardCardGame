package it.unibo.pps.wizard.engine.model.rules

import it.unibo.pps.wizard.engine.model.basic._
import it.unibo.pps.wizard.engine.model.basic.bidding.Bid
import it.unibo.pps.wizard.engine.model.basic.bidding.Bids
import it.unibo.pps.wizard.engine.model.basic.bidding.Trick
import it.unibo.pps.wizard.engine.model.basic.bidding.Tricks
import it.unibo.pps.wizard.engine.model.basic.gameplay.Round
import it.unibo.pps.wizard.engine.model.basic.scoreboard.Score
import it.unibo.pps.wizard.engine.model.basic.scoreboard.Scoreboard

/** Rules and calculations governing the scoring phase at the end of a round. */
object ScoringRules:
  // Official Wizard rules scoring constants
  private final val BASE_WIN_POINTS = 20
  private final val POINTS_PER_TRICK = 10

  /**
   * Calculates the cumulative scores for all players at the end of a round and updates the scoreboard.
   *
   * @param players    the list of players.
   * @param bids       the bids placed for this round.
   * @param tricks     the tricks won by each player in this round.
   * @param round      the current game round.
   * @param scoreboard the current scoreboard before adding this round's points.
   * @return the updated [[Scoreboard]] containing the new cumulative scores.
   */
  def compute(
      players: Players,
      bids: Bids,
      tricks: Tricks,
      round: Round,
      scoreboard: Scoreboard
  ): Scoreboard =
    players.toList.foldLeft(scoreboard): (sb, player) =>
      val bid = bids(player.id)
      val tricksWon = tricks(player.id)
      val roundPoints = bid.calculatePointsFor(tricksWon)

      val previousRoundNum = round.value - 1
      val previousScore =
        if previousRoundNum > 0 then sb.getStatsForRound(Round(previousRoundNum), player.id)._1
        else Score(0)

      val cumulativePoints = Score(previousScore + roundPoints)

      sb.addScore(player.id, round, cumulativePoints, bid)

  extension (bid: Bid)
    /**
     * Calculates the score obtained for a round based on the bid and the actual tricks won.
     * Gains points if the bid is exactly met, otherwise loses points proportional to the difference.
     *
     * @param tricksWon the number of tricks actually won by the player.
     * @return the [[Score]] gained (or lost) in the round.
     */
    def calculatePointsFor(tricksWon: Trick): Score =
      val points =
        if bid == tricksWon
        then BASE_WIN_POINTS + (tricksWon * POINTS_PER_TRICK)
        else -Math.abs(bid - tricksWon) * POINTS_PER_TRICK
      Score(points)

export ScoringRules.*
