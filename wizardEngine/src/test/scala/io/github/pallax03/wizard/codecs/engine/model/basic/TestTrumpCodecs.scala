package io.github.pallax03.wizard.codecs.engine.model.basic

import io.github.pallax03.wizard.codecs.syntax.CodecSyntax.*
import io.github.pallax03.wizard.engine.model.basic.*

import org.scalatest.EitherValues.*
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class TestTrumpCodecs extends AnyWordSpec with Matchers:

  import cards.Card.*
  import gameplay.Trump
  import TrumpCodecs.given

  "TrumpCodecs" should:
    "encode and decode Trump.Absent correctly" in:
      val trump: Trump = Trump.Absent
      val jsonString = trump.toJson
      jsonString shouldBe """{"type":"Absent"}"""
      jsonString.decodeAs[Trump].value shouldBe trump

    "encode and decode Trump.Jester correctly" in:
      val trump: Trump = Trump(jester)
      val jsonString = trump.toJson
      jsonString should fullyMatch regex """\{"type":"Jester","card":\{"type":"Jester","id":[0-3]\}\}"""
      jsonString.decodeAs[Trump].value shouldBe trump

    "encode and decode Trump.Standard correctly" in:
      val trump: Trump = Trump(Seven of Blue)
      val jsonString = trump.toJson
      jsonString shouldBe """{"type":"Standard","card":{"type":"Standard","color":"Blue","rank":7},"color":"Blue"}"""
      jsonString.decodeAs[Trump].value shouldBe trump

    "encode and decode Trump.WizardUnresolved correctly" in:
      val trump: Trump = Trump(wizard)
      val jsonString = trump.toJson
      jsonString should fullyMatch regex """\{"type":"WizardUnresolved","card":\{"type":"Wizard","id":[0-3]\}\}"""
      jsonString.decodeAs[Trump].value shouldBe trump

    "encode and decode Trump.WizardResolved correctly" in:
      val trump = (Trump(wizard) resolveWizard Color.Green).value
      val jsonString = trump.toJson
      jsonString should fullyMatch regex """\{"type":"WizardResolved","card":\{"type":"Wizard","id":[0-3]\},"color":"Green"\}"""
      jsonString.decodeAs[Trump].value shouldBe trump
