package io.github.pallax03.wizard.engine.model.basic.bidding

import io.github.pallax03.wizard.engine.model.basic.*

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class TestTricks extends AnyWordSpec with Matchers:

  "Tricks" when:
    val p1: PlayerId = PlayerId(1)
    val p2: PlayerId = PlayerId(2)
    "empty" should:
      "return zero for any player" in:
        Tricks.empty(p1) shouldBe 0

    "receiving a trick" should:
      val t = Tricks.empty addTrickTo p1
      "increment the count for the player" in:
        t(p1) shouldBe 1
      "accumulate correctly over multiple tricks" in:
        val newTricks = t addTrickTo p1 addTrickTo p2
        newTricks(p1) shouldBe 2
        newTricks(p2) shouldBe 1
