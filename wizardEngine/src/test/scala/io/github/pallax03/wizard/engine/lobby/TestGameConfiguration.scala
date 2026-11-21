package io.github.pallax03.wizard.engine.lobby

import io.github.pallax03.wizard.engine.model.basic.PlayerId

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class TestGameConfiguration extends AnyWordSpec with Matchers:

  "A GameConfiguration" when:
    "validated" should:
      "accept default and boundary parameters" in:
        GameConfiguration().validate shouldBe Right(())
        GameConfiguration(timer = 15, maxStrikes = 1).validate shouldBe Right(())
        GameConfiguration(timer = 90, maxStrikes = 3).validate shouldBe Right(())

      "reject timer outside [15, 90]" in:
        GameConfiguration(timer = 14).validate shouldBe Left(ConfigurationErrors.TimerNotValid)
        GameConfiguration(timer = 91).validate shouldBe Left(ConfigurationErrors.TimerNotValid)

      "reject maxStrikes outside [1, 3]" in:
        GameConfiguration(maxStrikes = 0).validate shouldBe Left(
          ConfigurationErrors.MaxStrikesNotValid
        )
        GameConfiguration(maxStrikes = 4).validate shouldBe Left(
          ConfigurationErrors.MaxStrikesNotValid
        )

    "calculating turn TTL" should:
      val config = GameConfiguration(timer = 30)

      "include the 3-second grace period at 0 strikes" in:
        config.calculateTTL(0) shouldBe 33

      "halve the TTL for each consecutive strike down to 1 second" in:
        config.calculateTTL(1) shouldBe 16
        config.calculateTTL(2) shouldBe 8
        config.calculateTTL(10) shouldBe 1

    "calculating player turn TTL" should:
      val defaultConfig = GameConfiguration(timer = 30)
      val slowConfig = GameConfiguration(timer = 90)
      val human = Player.human(PlayerId(0), "Alice", Some("secret"))
      val nativeBot = Player.bot(PlayerId(1), BotsDifficulty.Prolog)
      val replacedHuman = human.replaceWithABot()

      "apply strike decay for active human players" in:
        defaultConfig.calculateTTL(human) shouldBe 33
        defaultConfig.calculateTTL(human.copy(strikes = 1)) shouldBe 16
        defaultConfig.calculateTTL(human.copy(strikes = 2)) shouldBe 8

      "use dedicated bot timeout for native bots and AFK-replaced humans" in:
        defaultConfig.calculateTTL(nativeBot) shouldBe GameConfiguration.BOT_TURN_TIMEOUT_SECONDS
        slowConfig.calculateTTL(nativeBot) shouldBe GameConfiguration.BOT_TURN_TIMEOUT_SECONDS
        slowConfig.calculateTTL(replacedHuman) shouldBe GameConfiguration.BOT_TURN_TIMEOUT_SECONDS

      "cap bot timeout at the baseline human TTL when configured timer is lower" in:
        GameConfiguration(timer = 5).calculateTTL(nativeBot) shouldBe 8
