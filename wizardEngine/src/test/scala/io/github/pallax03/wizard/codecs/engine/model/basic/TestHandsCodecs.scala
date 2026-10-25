package io.github.pallax03.wizard.codecs.engine.model.basic

import io.github.pallax03.wizard.codecs.syntax.CodecSyntax.*
import io.github.pallax03.wizard.engine.model.basic.BasicTestDSL.*
import io.github.pallax03.wizard.engine.model.basic.PlayerId
import io.github.pallax03.wizard.engine.model.basic.cards.Card.*
import io.github.pallax03.wizard.engine.model.basic.cards.Hands

import org.scalatest.EitherValues.*
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class TestHandsCodecs extends AnyWordSpec with Matchers:

  import HandsCodecs.given

  "HandsCodecs" should:
    "encode and decode Hands correctly" in:
      val p1 = PlayerId(1)
      val p2 = PlayerId(2)
      val hands = handsOf(
        p1 holds (Ten of Blue),
        p2 holds (Thirteen of Yellow)
      )
      val jsonString = hands.toJson
      jsonString shouldBe """{"1":[{"type":"Standard","color":"Blue","rank":10}],"2":[{"type":"Standard","color":"Yellow","rank":13}]}"""
      jsonString.decodeAs[Hands].value shouldBe hands
