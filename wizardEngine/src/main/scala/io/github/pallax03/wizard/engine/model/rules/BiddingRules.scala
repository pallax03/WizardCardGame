package io.github.pallax03.wizard.engine.model.rules

import io.github.pallax03.wizard.engine.model.basic._
import io.github.pallax03.wizard.engine.model.basic.bidding.Bid
import io.github.pallax03.wizard.engine.model.basic.bidding.Bid._
import io.github.pallax03.wizard.engine.model.basic.bidding.Bids
import io.github.pallax03.wizard.engine.model.basic.gameplay.Round
import io.github.pallax03.wizard.engine.model.core.GameError

/** Rules and validations governing the bidding phase of a round. */
object BiddingRules:

  /**
   * Validates and processes a player's bid, adding it to the current bids if valid.
   *
   * @param bid           the bid value to be processed.
   * @param currentBids   the bids placed so far in this round.
   * @param currentPlayer the ID of the player placing the bid.
   * @param round         the current game round.
   * @param totalPlayers  the total number of players in the game.
   * @return Right with the updated [[Bids]] if valid, Left with a [[GameError]] otherwise.
   */
  def processBid(
      bid: Bid,
      currentBids: Bids,
      currentPlayer: PlayerId,
      round: Round,
      totalPlayers: Int
  ): Either[GameError, Bids] =
    bid
      .validateBid(round, currentBids, totalPlayers)
      .map(_ => currentBids + (currentPlayer place bid))

  extension (bid: Bid)
    /**
     * Validates if a bid conforms to both boundary rules and the last-player restriction.
     *
     * @param round the current game round.
     * @param currentBids the bids placed so far in this round.
     * @param totalPlayers the total number of players in the game.
     * @return Right(()) if valid, Left with a [[GameError]] otherwise.
     */
    def validateBid(round: Round, currentBids: Bids, totalPlayers: Int): Either[GameError, Unit] =
      if !isWithinBounds(bid, round) then Left(GameError.InvalidBid)
      else if isLastPlayerInvalid(bid, round, currentBids, totalPlayers) then
        Left(GameError.InvalidBid)
      else Right(())

  private def isWithinBounds(bid: Bid, round: Round): Boolean =
    bid >= 0 && bid.isValid(round)

  private def isLastPlayerInvalid(
      bid: Bid,
      round: Round,
      currentBids: Bids,
      totalPlayers: Int
  ): Boolean =
    val isLastPlayer = currentBids.isComplete(totalPlayers - 1)
    isLastPlayer && (currentBids.total + bid) == round
export BiddingRules.*
