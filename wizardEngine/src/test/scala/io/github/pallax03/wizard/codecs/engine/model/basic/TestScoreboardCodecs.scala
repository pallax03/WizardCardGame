package io.github.pallax03.wizard.codecs.engine.model.basic

import io.github.pallax03.wizard.codecs.syntax.CodecSyntax._
import io.github.pallax03.wizard.engine.model.basic._
import org.scalatest.EitherValues._
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
      val jsonString = scoreboard.toJson
      jsonString shouldBe """{"1":[{"round":1,"score":20,"bid":0}],"2":[{"round":1,"score":-10,"bid":1}]}"""
      jsonString.decodeAs[Scoreboard].value shouldBe scoreboard
