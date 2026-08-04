package it.unibo.pps.wizard.engine.model.rules

import it.unibo.pps.wizard.engine.model.basic.*
import it.unibo.pps.wizard.engine.model.basic.bidding.Bids
import it.unibo.pps.wizard.engine.model.basic.bidding.Tricks
import it.unibo.pps.wizard.engine.model.basic.gameplay.Round

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class TestScoringRules extends AnyWordSpec with Matchers:
  val p0: PlayerId = PlayerId(0)
  val p1: PlayerId = PlayerId(1)
  val p2: PlayerId = PlayerId(2)
  val playersIds: List[PlayerId] = List(p0, p1, p2)
  val round: Round = Round(1)

  "ScoringRules calculation" when:
    "a player matches their bid" should:
      "award 20 points for 0 tricks" in:
        0.calculatePointsFor(0) shouldBe 20

      "award base 20 + 10 per trick for exact positive bid" in:
        2.calculatePointsFor(2) shouldBe 40

    "a player fails their bid" should:
      "deduct 10 points per trick of difference when winning more" in:
        1.calculatePointsFor(3) shouldBe -20

      "deduct 10 points per trick of difference when winning fewer" in:
        3.calculatePointsFor(0) shouldBe -30

  "ScoringRules integration" when:
    val round1 = Round(1)
    val round2 = Round(2)
    "simulating a multi-round game sequence" should:
      val bids1 = Bids.empty + (p0 -> 0) + (p1 -> 1) + (p2 -> 2)
      val tricks1 = Tricks.empty.addTrickTo(p1).addTrickTo(p2).addTrickTo(p2)
      val scoreboardAfterR1 = compute(playersIds, bids1, tricks1, round1, Scoreboard.empty)
      val bids2 = Bids.empty + (p0 -> 1) + (p1 -> 2) + (p2 -> 0)
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
