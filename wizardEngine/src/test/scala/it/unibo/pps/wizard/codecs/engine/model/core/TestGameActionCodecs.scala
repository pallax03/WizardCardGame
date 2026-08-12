package it.unibo.pps.wizard.codecs.engine.model.core

import it.unibo.pps.wizard.codecs.syntax.CodecSyntax.*
import it.unibo.pps.wizard.engine.model.basic._
import it.unibo.pps.wizard.engine.model.core.GameAction
import org.scalatest.EitherValues._
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class TestGameActionCodecs extends AnyWordSpec with Matchers:

  import cards.Card.*
  import GameActionCodecs.given

  "GameActionCodecs" should:
    "encode and decode ResolveTrumpColor correctly" in:
      val action: GameAction = GameAction.ResolveTrumpColor(PlayerId(1), Color.Red)
      val jsonString = action.toJson

      jsonString shouldBe """{"action":"ResolveTrumpColor","playerId":1,"color":"Red"}"""
      jsonString.decodeAs[GameAction].value shouldBe action

    "encode and decode PlaceBid correctly" in:
      val action: GameAction = GameAction.PlaceBid(PlayerId(2), 3)
      val jsonString = action.toJson

      jsonString shouldBe """{"action":"PlaceBid","playerId":2,"bid":3}"""
      jsonString.decodeAs[GameAction].value shouldBe action

    "encode and decode PlayCard correctly" in:
      val action: GameAction = GameAction.PlayCard(PlayerId(1), Seven of Blue)
      val jsonString = action.toJson

      jsonString shouldBe """{"action":"PlayCard","playerId":1,"card":{"type":"Standard","color":"Blue","rank":7}}"""
      jsonString.decodeAs[GameAction].value shouldBe action
