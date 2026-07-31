package it.unibo.pps.wizard.engine.model.basic.bidding

import it.unibo.pps.wizard.engine.model.basic._
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class TestTricks extends AnyWordSpec with Matchers:

  import Tricks.*

  val p1: PlayerId = PlayerId(1)
  val name1: PlayerName = PlayerName("Alice")
  val p2: PlayerId = PlayerId(2)
  val name2: PlayerName = PlayerName("Bob")
  val players: Players = Players(Player.human(p1, name1), Player.human(p2, name2))

  "Tricks" when:
    "initialized" should:
      val tricks = Tricks.initialize(players)
      "set all players to zero" in:
        tricks(p1) shouldBe 0
        tricks(p2) shouldBe 0

    "empty" should:
      "return zero for any player" in:
        Tricks.empty(p1) shouldBe 0

    "receiving a trick" should:
      "increment the count for the player" in:
        val t = Tricks.initialize(players).addTrickTo(p1)
        t(p1) shouldBe 1
        t(p2) shouldBe 0

      "accumulate correctly over multiple tricks" in:
        val t = Tricks
          .initialize(players)
          .addTrickTo(p1)
          .addTrickTo(p1)
          .addTrickTo(p2)
        t(p1) shouldBe 2
        t(p2) shouldBe 1
