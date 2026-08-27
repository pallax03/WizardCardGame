package io.github.pallax03.wizard.engine.model.rules

import io.github.pallax03.wizard.engine.model.basic._
import io.github.pallax03.wizard.engine.model.basic.bidding.Bids
import io.github.pallax03.wizard.engine.model.basic.bidding.Tricks
import io.github.pallax03.wizard.engine.model.basic.gameplay.Round

/** Rules and calculations governing the scoring phase at the end of a round. */
object ScoringRules:
  private final val BASE_WIN_POINTS = 20
  private final val POINTS_PER_TRICK = 10

  /**
   * Calculates the cumulative scores for all players at the end of a round and updates the scoreboard.
   *
   * @param playersIds the list of players ids.
   * @param bids       the bids placed for this round.
   * @param tricks     the tricks won by each player in this round.
   * @param round      the current game round.
   * @param scoreboard the current scoreboard before adding this round's points.
   * @return the updated [[Scoreboard]] containing the new cumulative scores.
   */
  def compute(
      playersIds: List[PlayerId],
      bids: Bids,
      tricks: Tricks,
      round: Round,
      scoreboard: Scoreboard
  ): Scoreboard =
    playersIds.foldLeft(scoreboard): (sb, playerId) =>
      val bid = bids(playerId)
      val tricksWon = tricks(playerId)

      val pointsGained =
        if bid == tricksWon then BASE_WIN_POINTS + (tricksWon * POINTS_PER_TRICK)
        else -Math.abs(bid - tricksWon) * POINTS_PER_TRICK

      val cumulativePoints =
        sb.getStatsForRound(round - 1, playerId)._1 + pointsGained

      sb.addScore(playerId, round, cumulativePoints, bid)

export ScoringRules.*
