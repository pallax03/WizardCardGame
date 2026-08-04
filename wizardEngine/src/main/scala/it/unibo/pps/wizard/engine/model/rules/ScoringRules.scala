package it.unibo.pps.wizard.engine.model.rules

import it.unibo.pps.wizard.engine.model.basic.*
import it.unibo.pps.wizard.engine.model.basic.bidding.Bid
import it.unibo.pps.wizard.engine.model.basic.bidding.Bids
import it.unibo.pps.wizard.engine.model.basic.bidding.Trick
import it.unibo.pps.wizard.engine.model.basic.bidding.Tricks
import it.unibo.pps.wizard.engine.model.basic.gameplay.Round

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
      val cumulativePoints =
        sb.getStatsForRound(Round(round.value - 1), playerId)._1
          + bid.calculatePointsFor(tricks(playerId))
      sb.addScore(playerId, round, cumulativePoints, bid)

  extension (bid: Bid)
    /**
     * Calculates the score obtained for a round based on the bid and the actual tricks won.
     * Gains points if the bid is exactly met, otherwise loses points proportional to the difference.
     *
     * @param tricksWon the number of tricks actually won by the player.
     * @return the [[Score]] gained (or lost) in the round.
     */
    def calculatePointsFor(tricksWon: Trick): Score = if bid == tricksWon
    then BASE_WIN_POINTS + (tricksWon * POINTS_PER_TRICK)
    else -Math.abs(bid - tricksWon) * POINTS_PER_TRICK

export ScoringRules.*
