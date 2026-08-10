package it.unibo.pps.wizard.codecs.engine.model.basic

import io.circe.parser._
import io.circe.syntax._
import it.unibo.pps.wizard.engine.model.basic._
import org.scalatest.EitherValues._
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class TestPlayerIdCodecs extends AnyWordSpec with Matchers:

  import PlayerIdCodecs.given
  
  "PlayerIdCodecs" should:
    val p1 = PlayerId(1)
    "encode and decode PlayerId correctly" in:
      val jsonString = p1.asJson.noSpaces
      jsonString shouldBe "1"
      decode[PlayerId](jsonString).value shouldBe p1
