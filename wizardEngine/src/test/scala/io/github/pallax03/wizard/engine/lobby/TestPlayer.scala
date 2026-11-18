package io.github.pallax03.wizard.engine.lobby

import io.github.pallax03.wizard.engine.model.basic.PlayerId

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class TestPlayer extends AnyWordSpec with Matchers:

  import LobbyTestDSL.*

  "A Player" when:
    val p1 = PlayerId(1)

    "created as human" should:
      val player = human(p1)

      "have human identity and active control" in:
        player.isHuman shouldBe true
        player.isBot shouldBe false
        player.isHumanPlaying shouldBe true

    "created as bot" should:
      val player = bot(PlayerId(2))

      "be controlled by AI and remain a bot on returnHuman" in:
        player.isHuman shouldBe false
        player.isBot shouldBe true
        player.isHumanPlaying shouldBe false
        player.returnHuman shouldBe player

    "temporarily replaced by a bot" should:
      val delegated = human(p1).replaceWithABot()

      "preserve human secret while delegating active control" in:
        delegated.isHuman shouldBe true
        delegated.isBot shouldBe true
        delegated.isHumanPlaying shouldBe false

      "reclaim active human control upon reconnecting" in:
        val reclaimed = delegated.returnHuman
        reclaimed.isOnline shouldBe true
        reclaimed.isBot shouldBe false
        reclaimed.isHumanPlaying shouldBe true
