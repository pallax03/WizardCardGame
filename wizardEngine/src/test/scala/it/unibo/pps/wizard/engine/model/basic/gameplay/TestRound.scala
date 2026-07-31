package it.unibo.pps.wizard.engine.model.basic.gameplay

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class TestRound extends AnyWordSpec with Matchers:
  import Round.*

  "A Round" when:
    "initialized" should:
      "start at 1" in:
        Round.start.value shouldBe 1

    "transitioning" should:
      "correctly move to the next round" in:
        val r1 = Round.start
        val r2 = r1.next
        r2.value shouldBe 2

      "support sequential progression" in:
        val r3 = Round.start.next.next.next
        r3.value shouldBe 4

    "compared" should:
      "maintain the correct integer value for game logic" in:
        val r = Round.start.next
        r.value shouldBe 2
        (r.value == 2) shouldBe true
