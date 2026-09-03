package io.github.pallax03.wizard.engine.model.rules

import io.github.pallax03.wizard.engine.model.basic.*
import io.github.pallax03.wizard.engine.model.core.GameError

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class TestBiddingRules extends AnyWordSpec with Matchers:

  import bidding.Bids
  import gameplay.Round
  "BiddingRules" when:
    val p1: PlayerId = PlayerId(1)
    val p2: PlayerId = PlayerId(2)

    val totalPlayers = 3
    val round2: Round = 2
    "a bid is outside the valid range" should:
      "reject negative bids" in:
        -1.validateBid(round2, Bids.empty, totalPlayers) shouldBe Left(GameError.InvalidBid)

      "reject bids exceeding the current round number" in:
        3.validateBid(round2, Bids.empty, totalPlayers) shouldBe Left(GameError.InvalidBid)

    "processing the final bid (Hook Rule)" should:
      val bidsAfterP1 = Bids.empty + (p1 place 1)
      val bidsAfterP2 = bidsAfterP1 + (p2 place 0)

      "reject the bid if it causes the total to equal the round number" in:
        1.validateBid(round2, bidsAfterP2, totalPlayers) shouldBe Left(GameError.InvalidBid)

      "allow the bid if the total is different from the round number" in:
        2.validateBid(round2, bidsAfterP2, totalPlayers) shouldBe Right(())

    "a bid is valid" should:
      "add the bid to the Bids collection" in:
        val result = processBid(1, Bids.empty, p1, round2, totalPlayers)
        result shouldBe Right(Bids.empty + (p1 place 1))

      "correctly identify when a bid is within bounds" in:
        1.validateBid(round2, Bids.empty, totalPlayers) shouldBe Right(())
