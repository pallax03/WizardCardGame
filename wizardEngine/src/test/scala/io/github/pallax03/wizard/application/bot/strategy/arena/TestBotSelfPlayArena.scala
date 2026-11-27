package io.github.pallax03.wizard.application.bot.strategy.arena

import scala.concurrent.ExecutionContext.Implicits.global

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class TestBotSelfPlayArena extends AnyWordSpec with Matchers:

  "BotSelfPlayArena" should:
    "run a full homogeneous DumbBotStrategy match cleanly" in:
      val stats = (4 x dumb).play(deals = 1, seed = 7L)
      stats should have size 1
      stats.head.gamesPlayed shouldBe 16

    "run a homogeneous PrologBotStrategy tournament with zero fallbacks" in:
      val stats = (4 x prolog).play(deals = 1, seed = 11L)
      stats should have size 1
      stats.head.aiFallbacks shouldBe 0

    "benchmark PrologBotStrategy against DumbBotStrategy on duplicate deals" in:
      val stats = (prolog vs dumb).play(deals = 3)
      info("\n" + stats.report)

      val prologStats = stats.find(_.name == ArenaSeat.prologName).get
      val dumbStats = stats.find(_.name == ArenaSeat.dumbName).get
      prologStats.aiFallbacks shouldBe 0
      prologStats.avgScore should be > dumbStats.avgScore
