package it.unibo.pps.wizard.codecs.engine.model.basic

import io.circe.parser.*
import io.circe.syntax.*
import it.unibo.pps.wizard.engine.model.basic.*

import org.scalatest.EitherValues.*
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class TestScoreboardCodecs extends AnyWordSpec with Matchers:

  import ScoreboardCodecs.given

  "ScoreboardCodecs" should:
    val p1 = PlayerId(1)
    val p2 = PlayerId(2)
    "encode and decode empty Table correctly" in:
      val scoreboard = Scoreboard.empty
        .addScore(p1, 1, 20, 0)
        .addScore(p2, 1, -10, 1)
      val jsonString = scoreboard.asJson.noSpaces
      jsonString shouldBe """{"1":{"1":[20,0]},"2":{"1":[-10,1]}}"""
      decode[Scoreboard](jsonString).value shouldBe scoreboard