package it.unibo.pps.wizard.codecs.engine.model.basic

import io.circe.parser._
import io.circe.syntax._
import it.unibo.pps.wizard.engine.model.basic._
import org.scalatest.EitherValues._
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class TestTrumpCodecs extends AnyWordSpec with Matchers:

  import cards.Card.*
  import gameplay.Trump
  "TrumpCodecs" should:
    import TrumpCodecs.given
    "encode and decode Trump.Absent correctly" in:
      val trump: Trump = Trump.Absent
      val jsonString = trump.asJson.noSpaces
      jsonString shouldBe """{"type":"Absent"}"""
      decode[Trump](jsonString).value shouldBe trump

    "encode and decode Trump.Jester correctly" in:
      val trump: Trump = Trump(jester)
      val jsonString = trump.asJson.noSpaces
      jsonString should fullyMatch regex """\{"type":"Jester","card":\{"type":"Jester","id":[0-3]\}\}"""
      decode[Trump](jsonString).value shouldBe trump

    "encode and decode Trump.Standard correctly" in:
      val trump: Trump = Trump(Seven of Blue)
      val jsonString = trump.asJson.noSpaces
      jsonString shouldBe """{"type":"Standard","card":{"type":"Standard","color":"Blue","rank":7},"color":"Blue"}"""
      decode[Trump](jsonString).value shouldBe trump

    "encode and decode Trump.WizardUnresolved correctly" in:
      val trump: Trump = Trump(wizard)
      val jsonString = trump.asJson.noSpaces
      jsonString should fullyMatch regex """\{"type":"WizardUnresolved","card":\{"type":"Wizard","id":[0-3]\}\}"""
      decode[Trump](jsonString).value shouldBe trump

    "encode and decode Trump.WizardResolved correctly" in:
      val trump = (Trump(wizard) resolveWizard Color.Green).value
      val jsonString = trump.asJson.noSpaces
      jsonString should fullyMatch regex """\{"type":"WizardResolved","card":\{"type":"Wizard","id":[0-3]\},"color":"Green"\}"""
      decode[Trump](jsonString).value shouldBe trump
