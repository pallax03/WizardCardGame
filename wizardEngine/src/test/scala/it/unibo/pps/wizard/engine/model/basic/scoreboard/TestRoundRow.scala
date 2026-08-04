package it.unibo.pps.wizard.engine.model.basic.scoreboard

import it.unibo.pps.wizard.engine.model.basic._
import it.unibo.pps.wizard.engine.model.basic.gameplay.Round
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class TestRoundRow extends AnyWordSpec with Matchers:
  import Scoreboard.*

  val p1: Player = Player.human(PlayerId(0), PlayerName("Alice"))
  val p2: Player = Player.human(PlayerId(1), PlayerName("Bob"))
  val p3: Player = Player.human(PlayerId(2), PlayerName("Charlie"))
  val players: Players = Players(p1, p2, p3)
  val r1: Round = Round(1)
  val r2: Round = Round(2)

  "A RoundRow" when:
    "querying scores and bids" should:
      "return empty strings if the player has no stats for that round" in:
        val row = RoundRow(r1, Map(p1.id -> None))
        row.getScore(p1.id) shouldBe ""
        row.getBid(p1.id) shouldBe ""

      "return empty strings if the player is entirely missing from the map" in:
        val row = RoundRow(r1, Map.empty)
        row.getScore(p1.id) shouldBe ""
        row.getBid(p1.id) shouldBe ""

      "return the correct string representation of score and bid when present" in:
        val stats = Map(p1.id -> Some((Score(10), 2)))
        val row = RoundRow(r1, stats)

        row.getScore(p1.id) shouldBe "10"
        row.getBid(p1.id) shouldBe "2"

  "RoundRow companion object" when:
    "generating stats for all players" should:
      "correctly map scoreboard data to options" in:
        val sb = Scoreboard.empty
          .addScore(p1.id, r1, Score(25), 1)

        val stats = RoundRow.getStatsForAllPlayers(r1, players, sb)

        stats(p1.id) shouldBe Some((Score(25), 1))
        stats(p2.id) shouldBe None
        stats(p3.id) shouldBe None

    "creating rows for the entire game" should:
      "generate the correct number of rows based on the player count" in:
        val rows = RoundRow.initRows(players)

        rows.size shouldBe 20
        rows.head.round shouldBe Round(1)
        rows.last.round shouldBe Round(20)

      "populate each row with the correct historical data from the scoreboard" in:
        val sb = Scoreboard.empty
          .addScore(p1.id, r1, Score(10), 1)
          .addScore(p2.id, r2, Score(20), 2)

        val rows = RoundRow.updateRows(players, sb)

        val row1 = rows.find(_.round == r1).get
        val row2 = rows.find(_.round == r2).get

        row1.getScore(p1.id) shouldBe "10"
        row1.getScore(p2.id) shouldBe ""

        row2.getScore(p1.id) shouldBe ""
        row2.getScore(p2.id) shouldBe "20"
