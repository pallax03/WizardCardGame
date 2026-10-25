package io.github.pallax03.wizard.engine.model.rules

import io.github.pallax03.wizard.engine.model.basic.*
import io.github.pallax03.wizard.engine.model.basic.bidding.Bid.*
import io.github.pallax03.wizard.engine.model.basic.bidding.{Bid, Bids}
import io.github.pallax03.wizard.engine.model.basic.gameplay.Round
import io.github.pallax03.wizard.engine.model.core.GameActionError

/**
 * Rules and validations governing the bidding phase of a round.
 *
 * In the Wizard Card Game, each player in turn predicts how many tricks they will win in that round:
 *   - Each bid must be between `0` and the current `round` number (inclusive).
 *   - **The Last-Player (Dealer) Rule**: The last player to bid cannot place a bid that causes
 *     the sum of all players' bids to equal the round number (`totalBids == round`). This ensures
 *     that at least one player will miss their bid, preventing ties where everyone succeeds.
 */
object BiddingRules:

  /** Validates and records a player's bid, returning the updated [[Bids]] or a [[GameActionError]]. */
  def processBid(
      bid: Bid,
      currentBids: Bids,
      currentPlayer: PlayerId,
      round: Round,
      totalPlayers: Int
  ): Either[GameActionError, Bids] =
    bid
      .validateBid(round, currentBids, totalPlayers)
      .map(_ => currentBids + (currentPlayer place bid))

  extension (currentBids: Bids)
    /**
     * Determines the forbidden bid value for the last player to bid, if one exists.
     *
     * Used by the engine to construct [[io.github.pallax03.wizard.engine.model.events.InvitationEvent.WaitingForBid]]
     * so that clients and bots can immediately know which bid value is disallowed.
     *
     * @param round        the current round number.
     * @param totalPlayers the total number of players in the match.
     * @return `Some(bid)` containing the forbidden bid for the dealer, or `None` if not the last bidder
     *         or if the forbidden value falls outside `[0, round]`.
     */
    def notValidBid(round: Round, totalPlayers: Int): Option[Bid] =
      val suspectedInvalid = round - currentBids.total
      Option
        .when(suspectedInvalid.validateBid(round, currentBids, totalPlayers).isLeft)(
          suspectedInvalid
        )
        .filter(isWithinBounds(_, round))

  extension (bid: Bid)
    /**
     * Validates whether a bid conforms to boundary rules `[0, round]` and the last-player restriction.
     *
     * @param round        the current game round.
     * @param currentBids  the bids already recorded in this round.
     * @param totalPlayers the total number of players in the match.
     * @return `Right(())` if legal, or `Left(GameActionError.InvalidBid)` if the bid violates rules.
     */
    def validateBid(
        round: Round,
        currentBids: Bids,
        totalPlayers: Int
    ): Either[GameActionError, Unit] =
      if !isWithinBounds(bid, round) then Left(GameActionError.InvalidBid(round, bid))
      else if isLastPlayerInvalid(bid, round, currentBids, totalPlayers) then
        Left(GameActionError.InvalidBid(round, bid))
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
