package it.unibo.pps.wizard.codecs.engine.model.basic

import io.circe.parser._, io.circe.syntax._

import it.unibo.pps.wizard.engine.model.basic.cards._

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class TestCardCodecs extends AnyWordSpec with Matchers:

  import Card.*

  "CardCodecs" should:
    import CardCodecs.given
    "encode and decode Card.Color correctly" in:
      val color = Color.Red
      val json = color.asJson
      json.as[Color] shouldBe Right(color)

    "encode and decode Card.Rank correctly" in:
      val rank = Rank.Ten
      val json = rank.asJson
      json.as[Rank] shouldBe Right(rank)

    "encode and decode Card.Standard correctly" in:
      val card: Card = Seven of Blue
      val jsonString = card.asJson.noSpaces
      jsonString shouldBe """{"type":"Standard","color":"Blue","rank":7}"""
      decode[Card](jsonString) shouldBe Right(card)

    "encode and decode Card.Wizard correctly" in:
      val card: Card = wizard
      val jsonString = card.asJson.noSpaces
      jsonString should fullyMatch regex """\{"type":"Wizard","id":[0-3]\}"""
      decode[Card](jsonString) shouldBe Right(card)

    "encode and decode Card.Jester correctly" in:
      val card: Card = jester
      val jsonString = card.asJson.noSpaces
      jsonString should fullyMatch regex """\{"type":"Jester","id":[0-3]\}"""
      decode[Card](jsonString) shouldBe Right(card)
