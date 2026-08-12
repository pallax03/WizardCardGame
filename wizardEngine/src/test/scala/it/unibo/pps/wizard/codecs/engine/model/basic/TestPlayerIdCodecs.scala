package it.unibo.pps.wizard.codecs.engine.model.basic

import it.unibo.pps.wizard.codecs.syntax.CodecSyntax.*
import it.unibo.pps.wizard.engine.model.basic._
import org.scalatest.EitherValues._
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class TestPlayerIdCodecs extends AnyWordSpec with Matchers:

  import PlayerIdCodecs.given
  
  "PlayerIdCodecs" should:
    val p1 = PlayerId(1)
    "encode and decode PlayerId correctly" in:
      val jsonString = p1.toJsonString
      jsonString shouldBe "1"
      jsonString.decodeAs[PlayerId].value shouldBe p1
