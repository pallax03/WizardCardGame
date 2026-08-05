package it.unibo.pps.wizard.codecs.engine.model.basic

import io.circe.parser.*
import io.circe.syntax.*

import it.unibo.pps.wizard.engine.model.basic.*

import org.scalatest.EitherValues.*
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class TestPlayerCodecs extends AnyWordSpec with Matchers:

  "PlayerCodecs" should:
    import PlayerCodecs.given
    "encode and decode PlayerId correctly" in:
      val pId = PlayerId(42)
      val jsonString = pId.asJson.noSpaces
      
      jsonString shouldBe "42"
      decode[PlayerId](jsonString).value shouldBe pId
