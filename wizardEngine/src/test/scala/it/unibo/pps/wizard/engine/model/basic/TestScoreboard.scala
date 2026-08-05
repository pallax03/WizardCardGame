package it.unibo.pps.wizard.engine.model.basic

import it.unibo.pps.wizard.engine.model.basic._
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class TestScoreboard extends AnyWordSpec with Matchers:
  import bidding.Bid
  import gameplay.Round

  "A Scoreboard" when:
    val p1Id: PlayerId = PlayerId(1)
    val p2Id: PlayerId = PlayerId(2)

    val r1: Round = 1
    val r2: Round = 2
    val b1: Bid = 1
    val b2: Bid = 2
    "empty" should:
      val sb = Scoreboard.empty

      "return default stats (0 points, 0 bid) for any unplayed round" in:
        val (score, bid) = sb.getStatsForRound(r1, p1Id)
        score shouldBe 0
        bid shouldBe 0

    "updated with addScore" should:
      "correctly store score and bid for a player in a specific round" in:
        val sb = Scoreboard.empty.addScore(p1Id, r1, 20, b1)
        val (score, bid) = sb.getStatsForRound(r1, p1Id)

        score shouldBe 20
        bid shouldBe 1

      "store distinct stats across multiple rounds for the same player" in:
        val sb = Scoreboard.empty
          .addScore(p1Id, r1, 20, b1)
          .addScore(p1Id, r2, 10, b2)

        val (scoreR1, bidR1) = sb.getStatsForRound(r1, p1Id)
        scoreR1 shouldBe 20
        bidR1 shouldBe 1

        val (scoreR2, bidR2) = sb.getStatsForRound(r2, p1Id)
        scoreR2 shouldBe 10
        bidR2 shouldBe 2

      "maintain distinct scores and bids for different players" in:
        val sb = Scoreboard.empty
          .addScore(p1Id, r1, 50, b1)
          .addScore(p2Id, r1, -10, b2)

        sb.getStatsForRound(r1, p1Id)._1 shouldBe 50
        sb.getStatsForRound(r1, p2Id)._1 shouldBe -10

    "handling negative values" should:
      "allow negative scores to be recorded for a round" in:
        val sb = Scoreboard.empty.addScore(p1Id, r1, -15, b1)

        val (score, _) = sb.getStatsForRound(r1, p1Id)
        score shouldBe -15
