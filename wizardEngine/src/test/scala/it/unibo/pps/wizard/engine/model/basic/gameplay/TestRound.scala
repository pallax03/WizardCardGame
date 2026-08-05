package it.unibo.pps.wizard.engine.model.basic.gameplay

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class TestRound extends AnyWordSpec with Matchers:
  import Round.*

  "A Round" when:
    "initialized" should:
      val round: Round = Round.start
      "start at 1" in:
        round shouldBe 1
      "is a Int" in:
        round shouldBe a[Int]
    
    "transitioning" should:
      "correctly move to the next round" in:
        val r1 = Round.start
        val r2 = r1.next
        r2 shouldBe 2
