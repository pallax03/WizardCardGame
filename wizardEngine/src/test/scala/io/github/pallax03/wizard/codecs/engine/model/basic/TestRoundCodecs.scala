package io.github.pallax03.wizard.codecs.engine.model.basic

import io.github.pallax03.wizard.codecs.syntax.CodecSyntax.*
import io.github.pallax03.wizard.engine.model.basic.gameplay.Round

import org.scalatest.EitherValues.*
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class TestRoundCodecs extends AnyWordSpec with Matchers:

  import RoundCodecs.given

  "RoundCodecs" should:
    "encode and decode Round as a map key correctly" in:
      val roundMap: Map[Round, String] = Map(5 -> "five")
      val jsonString = roundMap.toJson
      jsonString shouldBe """{"5":"five"}"""
      jsonString.decodeAs[Map[Round, String]].value shouldBe roundMap
