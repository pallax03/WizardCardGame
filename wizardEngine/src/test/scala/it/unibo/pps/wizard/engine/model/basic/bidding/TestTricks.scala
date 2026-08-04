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
    "empty" should:
      "return zero for any player" in:
        Tricks.empty(p1) shouldBe 0

    "receiving a trick" should:
      val t = Tricks.empty.addTrickTo(p1)
      "increment the count for the player" in:
        t(p1) shouldBe 1
      "accumulate correctly over multiple tricks" in:
        val newTricks = t.addTrickTo(p1).addTrickTo(p2)
        newTricks(p1) shouldBe 2
        newTricks(p2) shouldBe 1
