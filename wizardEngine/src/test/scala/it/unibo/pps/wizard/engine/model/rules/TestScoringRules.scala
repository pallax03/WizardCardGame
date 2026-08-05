package it.unibo.pps.wizard.engine.model.rules

import it.unibo.pps.wizard.engine.model.basic._
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class TestScoringRules extends AnyWordSpec with Matchers:

  import bidding._
  import gameplay.Round
  "ScoringRules integration" when:
    val p0: PlayerId = PlayerId(0)
    val p1: PlayerId = PlayerId(1)
    val p2: PlayerId = PlayerId(2)
    val playersIds: List[PlayerId] = List(p0, p1, p2)

    val round1: Round = 1
    val round2: Round = 2
    "simulating a multi-round game sequence" should:
      val bids1 = Bids.empty + (p0 place 0) + (p1 place 1) + (p2 place 2)
      val tricks1 = Tricks.empty.addTrickTo(p1).addTrickTo(p2).addTrickTo(p2)
      val scoreboardAfterR1 = compute(playersIds, bids1, tricks1, round1, Scoreboard.empty)
      val bids2 = Bids.empty + (p0 place 1) + (p1 place 2) + (p2 place 0)
      val tricks2 = Tricks.empty.addTrickTo(p0).addTrickTo(p2).addTrickTo(p2)
      val scoreboardAfterR2 = compute(playersIds, bids2, tricks2, round2, scoreboardAfterR1)

      "calculate baseline points correctly for Round 1" in:
        scoreboardAfterR1(p0)(round1) shouldBe (20, 0)
        scoreboardAfterR1(p1)(round1) shouldBe (30, 1)
        scoreboardAfterR1(p2)(round1) shouldBe (40, 2)

      "add round 2 gains to the previous baseline for successful bids" in:
        scoreboardAfterR2(p0)(round2) shouldBe (50, 1)

      "deduct round 2 penalties from the previous baseline for failed bids" in:
        scoreboardAfterR2(p1)(round2) shouldBe (10, 2)
        scoreboardAfterR2(p2)(round2) shouldBe (20, 0)

      "keep historical records for older rounds intact after the new round is processed" in:
        scoreboardAfterR2(p0)(round1)._1 shouldBe 20
