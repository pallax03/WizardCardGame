package io.github.pallax03.wizard.engine.model.basic.bidding

import io.github.pallax03.wizard.engine.model.basic.*

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class TestBids extends AnyWordSpec with Matchers:

  import bidding.Bid.*
  import gameplay.Round
  import gameplay.Round.*

  "A Bid" when:
    "validated" should:
      "be valid if within round range" in:
        val round = Round.start.next
        2.isValid(round) shouldBe true
        3.isValid(round) shouldBe false

  "Bids collection" when:
    val p1: PlayerId = PlayerId(1)
    val p2: PlayerId = PlayerId(2)
    "empty" should:
      val bids = Bids.empty
      "return zero for any player" in:
        bids(p1) shouldBe 0

      "have size zero and total zero" in:
        bids.isComplete(3) shouldBe false
        bids.total shouldBe 0

    "receiving bids" should:
      val bids = Bids.empty + (p1 place 1) + (p2 place 2)
      "store and retrieve bids correctly" in:
        bids(p1) shouldBe 1
        bids(p2) shouldBe 2

      "calculate the correct total" in:
        bids.total shouldBe 3

      "identify when complete" in:
        bids.isComplete(2) shouldBe true
        bids.isComplete(3) shouldBe false
