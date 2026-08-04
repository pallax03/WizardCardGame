package it.unibo.pps.wizard.engine.model.basic.bidding

import it.unibo.pps.wizard.engine.model.basic.PlayerId
import it.unibo.pps.wizard.engine.model.basic.bidding.Bid
import it.unibo.pps.wizard.engine.model.basic.bidding.Bids
import it.unibo.pps.wizard.engine.model.basic.gameplay.Round
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class TestBids extends AnyWordSpec with Matchers:
  import Bid.*

  val p1: PlayerId = PlayerId(1)
  val p2: PlayerId = PlayerId(2)
  "A Bid" when:
    "validated" should:
      "be valid if within round range" in:
        val round = Round.start.next
        2.isValid(round) shouldBe true
        3.isValid(round) shouldBe false

  "Bids collection" when:
    import Bids.*
    "empty" should:
      val bids = Bids.empty
      "return zero for any player" in:
        bids(p1) shouldBe 0

      "have size zero and total zero" in:
        bids.isComplete(3) shouldBe false
        bids.total shouldBe 0

    "receiving bids" should:
      val bids = Bids.empty + (p1 -> 1) + (p2 -> 2)
      "store and retrieve bids correctly" in:
        bids(p1) shouldBe 1
        bids(p2) shouldBe 2

      "calculate the correct total" in:
        bids.total shouldBe 3

      "identify when complete" in:
        bids.isComplete(2) shouldBe true
        bids.isComplete(3) shouldBe false
