package it.unibo.pps.wizard.engine.model.basic.scoreboard

import it.unibo.pps.wizard.engine.model.basic._
import it.unibo.pps.wizard.engine.model.basic.bidding.Bid
import it.unibo.pps.wizard.engine.model.basic.gameplay.Round
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class TestScoreboard extends AnyWordSpec with Matchers:
  import Scoreboard.*

  val p1Id: PlayerId = PlayerId(1)
  val p2Id: PlayerId = PlayerId(2)

  val p1: Player = Player.human(p1Id, PlayerName("Alice"))
  val p2: Player = Player.human(p2Id, PlayerName("Bob"))
  val players: Players = Players(p1, p2)

  val r1: Round = Round(1)
  val r2: Round = Round(2)
  val r3: Round = Round(3)
  val b1: Bid = 1
  val b2: Bid = 2

  "A Scoreboard" when:
    "empty" should:
      val sb = Scoreboard.empty

      "return an empty history map for any player" in:
        sb(p1Id) shouldBe Map.empty
        sb(p2Id) shouldBe Map.empty

      "return default stats (0 points, 0 bid) for any unplayed round" in:
        val (score, bid) = sb.getStatsForRound(r1, p1Id)
        score shouldBe 0
        bid shouldBe 0

    "updated with addScore" should:
      "correctly store score and bid for a player in a specific round" in:
        val sb = Scoreboard.empty.addScore(p1Id, r1, Score(20), b1)
        val (score, bid) = sb.getStatsForRound(r1, p1Id)

        score shouldBe 20
        bid shouldBe 1

      "store distinct stats across multiple rounds for the same player" in:
        val sb = Scoreboard.empty
          .addScore(p1Id, r1, Score(20), b1)
          .addScore(p1Id, r2, Score(10), b2)

        val (scoreR1, bidR1) = sb.getStatsForRound(r1, p1Id)
        scoreR1 shouldBe 20
        bidR1 shouldBe 1

        val (scoreR2, bidR2) = sb.getStatsForRound(r2, p1Id)
        scoreR2 shouldBe 10
        bidR2 shouldBe 2

      "maintain distinct scores and bids for different players" in:
        val sb = Scoreboard.empty
          .addScore(p1Id, r1, Score(50), b1)
          .addScore(p2Id, r1, Score(-10), b2)

        sb.getStatsForRound(r1, p1Id)._1 shouldBe 50
        sb.getStatsForRound(r1, p2Id)._1 shouldBe -10

    "handling negative values" should:
      "allow negative scores to be recorded for a round" in:
        val sb = Scoreboard.empty.addScore(p1Id, r1, Score(-15), b1)

        val (score, _) = sb.getStatsForRound(r1, p1Id)
        score shouldBe -15

    "converting to RoundRows" should:
      "return rows sorted chronologically by round, regardless of insertion order" in:
        // Inseriamo i round in ordine sparso (R2, poi R3, poi R1)
        val sb = Scoreboard.empty
          .addScore(p1Id, r2, Score(30), 2)
          .addScore(p1Id, r3, Score(40), 1)
          .addScore(p1Id, r1, Score(10), 0)

        val rows = sb.toRoundRows(players)

        rows.size shouldBe 3
        rows.head.round shouldBe r1
        rows(1).round shouldBe r2
        rows(2).round shouldBe r3

  "Round Ordering" should:
    "correctly compare rounds based on their values" in:
      val roundOrdering = summon[Ordering[Round]]

      roundOrdering.compare(r1, r2) should clicksLessThanZero
      roundOrdering.compare(r2, r1) should clicksGreaterThanZero
      roundOrdering.compare(r1, r1) shouldBe 0

  private def clicksLessThanZero = be < 0
  private def clicksGreaterThanZero = be > 0
