package io.github.pallax03.wizard.engine.model.rules

import io.github.pallax03.wizard.engine.model.basic.*
import io.github.pallax03.wizard.engine.model.basic.bidding.{Bids, Tricks}
import io.github.pallax03.wizard.engine.model.basic.gameplay.Round

/**
 * Rules and calculations governing the scoring phase at the end of each round.
 *
 * Official Wizard scoring formulas:
 *   - **Correct Bid** (`bid == tricksWon`): The player is awarded 20 base points plus 10 points
 *     for each trick won:
 *     {{{
 *       points = 20 + (10 * tricksWon)
 *     }}}
 *   - **Incorrect Bid** (`bid != tricksWon`): The player loses 10 points for each trick above or below
 *     their prediction:
 *     {{{
 *       points = -10 * |bid - tricksWon|
 *     }}}
 *
 * Each round's score is added to the player's cumulative total on the [[Scoreboard]].
 */
object ScoringRules:
  private final val BASE_WIN_POINTS = 20
  private final val POINTS_PER_TRICK = 10

  /** Computes cumulative scores for all players at round end and updates the scoreboard. */
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
