package it.unibo.pps.wizard.engine.model.rules

import it.unibo.pps.wizard.engine.model.basic._
import it.unibo.pps.wizard.engine.model.basic.bidding.Bid
import it.unibo.pps.wizard.engine.model.basic.bidding.Bids
import it.unibo.pps.wizard.engine.model.basic.gameplay.Round
import it.unibo.pps.wizard.engine.model.core.GameError
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class TestBiddingRules extends AnyWordSpec with Matchers:
  val p1: PlayerId = PlayerId(1)
  val p2: PlayerId = PlayerId(2)
  val p3: PlayerId = PlayerId(3)
  val totalPlayers = 3
  val round2: Round = Round.start.next // Round 2

  "BiddingRules" when:
    "a bid is outside the valid range" should:
      "reject negative bids" in:
        Bid(-1).validateBid(round2, Bids.empty, totalPlayers) shouldBe Left(GameError.InvalidBid)

      "reject bids exceeding the current round number" in:
        Bid(3).validateBid(round2, Bids.empty, totalPlayers) shouldBe Left(GameError.InvalidBid)

    "processing the final bid (Hook Rule)" should:
      val bidsAfterP1 = Bids.empty + (p1 -> Bid(1))
      val bidsAfterP2 = bidsAfterP1 + (p2 -> Bid(0))

      "reject the bid if it causes the total to equal the round number" in:
        Bid(1).validateBid(round2, bidsAfterP2, totalPlayers) shouldBe Left(GameError.InvalidBid)

      "allow the bid if the total is different from the round number" in:
        Bid(2).validateBid(round2, bidsAfterP2, totalPlayers) shouldBe Right(())

    "a bid is valid" should:
      "add the bid to the Bids collection" in:
        val result = processBid(Bid(1), Bids.empty, p1, round2, totalPlayers)
        result shouldBe Right(Bids.empty + (p1 -> Bid(1)))

  "BiddingRules integration logic" should:
    "correctly identify when a bid is within bounds" in:
      Bid(1).validateBid(round2, Bids.empty, totalPlayers) shouldBe Right(())
