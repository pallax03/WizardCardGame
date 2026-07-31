package it.unibo.pps.wizard.engine.model.rules

import it.unibo.pps.wizard.engine.model.basic._
import it.unibo.pps.wizard.engine.model.basic.bidding.Bid
import it.unibo.pps.wizard.engine.model.basic.bidding.Bids
import it.unibo.pps.wizard.engine.model.basic.bidding.Trick
import it.unibo.pps.wizard.engine.model.basic.bidding.Tricks
import it.unibo.pps.wizard.engine.model.basic.gameplay.Round
import it.unibo.pps.wizard.engine.model.basic.scoreboard.Score
import it.unibo.pps.wizard.engine.model.basic.scoreboard.Scoreboard
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class TestScoringRules extends AnyWordSpec with Matchers:
  val p0: Player = Player.human(PlayerId(0), PlayerName("Alice"))
  val p1: Player = Player.human(PlayerId(1), PlayerName("Bob"))
  val p2: Player = Player.human(PlayerId(2), PlayerName("Charlie"))
  val players: Players = Players(p0, p1, p2)
  val round: Round = Round(1)

  "ScoringRules calculation" when:
    "a player matches their bid" should:
      "award 20 points for 0 tricks" in:
        Bid(0).calculatePointsFor(Trick(0)) shouldBe Score(20)

      "award base 20 + 10 per trick for exact positive bid" in:
        Bid(2).calculatePointsFor(Trick(2)) shouldBe Score(40)

    "a player fails their bid" should:
      "deduct 10 points per trick of difference when winning more" in:
        Bid(1).calculatePointsFor(Trick(3)) shouldBe Score(-20)

      "deduct 10 points per trick of difference when winning fewer" in:
        Bid(3).calculatePointsFor(Trick(0)) shouldBe Score(-30)

  "ScoringRules integration" when:
    val round1 = Round(1)
    val round2 = Round(2)
    "simulating a multi-round game sequence" should:
      val bids1 = Bids.empty + (p0.id -> Bid(0)) + (p1.id -> Bid(1)) + (p2.id -> Bid(2))
      val tricks1 = Tricks.initialize(players).addTrickTo(p1.id).addTrickTo(p2.id).addTrickTo(p2.id)
      val scoreboardAfterR1 = compute(players, bids1, tricks1, round1, Scoreboard.empty)
      val bids2 = Bids.empty + (p0.id -> Bid(1)) + (p1.id -> Bid(2)) + (p2.id -> Bid(0))
      val tricks2 = Tricks.initialize(players).addTrickTo(p0.id).addTrickTo(p2.id).addTrickTo(p2.id)
      val scoreboardAfterR2 = compute(players, bids2, tricks2, round2, scoreboardAfterR1)

      "calculate baseline points correctly for Round 1" in:
        scoreboardAfterR1(p0.id)(round1) shouldBe (Score(20), Bid(0))
        scoreboardAfterR1(p1.id)(round1) shouldBe (Score(30), Bid(1))
        scoreboardAfterR1(p2.id)(round1) shouldBe (Score(40), Bid(2))

      "add round 2 gains to the previous baseline for successful bids" in:
        scoreboardAfterR2(p0.id)(round2) shouldBe (Score(50), Bid(1))

      "deduct round 2 penalties from the previous baseline for failed bids" in:
        scoreboardAfterR2(p1.id)(round2) shouldBe (Score(10), Bid(2))
        scoreboardAfterR2(p2.id)(round2) shouldBe (Score(20), Bid(0))

      "keep historical records for older rounds intact after the new round is processed" in:
        scoreboardAfterR2(p0.id)(round1)._1 shouldBe 20
