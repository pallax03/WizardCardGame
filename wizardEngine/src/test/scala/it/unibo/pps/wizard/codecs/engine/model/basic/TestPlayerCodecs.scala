package it.unibo.pps.wizard.codecs.engine.model.basic

import io.circe.parser.*
import io.circe.syntax.*
import it.unibo.pps.wizard.engine.model.basic.{PlayerId, PlayerName, Player}
import org.scalatest.EitherValues.*
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class TestPlayerCodecs extends AnyWordSpec with Matchers:
  import PlayerCodecs.given

  "PlayerCodecs" should:
    "encode and decode PlayerId correctly" in:
      val pId = PlayerId(42)
      val jsonString = pId.asJson.noSpaces
      
      jsonString shouldBe "42"
      decode[PlayerId](jsonString).value shouldBe pId

    "encode and decode PlayerName correctly" in:
      val pName = PlayerName("Alice")
      val jsonString = pName.asJson.noSpaces
      jsonString shouldBe """"Alice""""
      decode[PlayerName](jsonString).value shouldBe pName

    "encode and decode Player correctly" in:
      val player = Player.human(PlayerId(1), PlayerName("Bob"))
      val jsonString = player.asJson.noSpaces
      
      jsonString shouldBe """{"id":1,"name":"Bob","isBot":false}"""
      decode[Player](jsonString).value shouldBe player
