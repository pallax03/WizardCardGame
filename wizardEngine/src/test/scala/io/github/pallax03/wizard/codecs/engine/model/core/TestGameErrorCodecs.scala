package io.github.pallax03.wizard.codecs.engine.model.core

import io.github.pallax03.wizard.codecs.syntax.CodecSyntax.*
import io.github.pallax03.wizard.engine.model.basic.*
import io.github.pallax03.wizard.engine.model.core.*

import org.scalatest.EitherValues.*
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class TestGameErrorCodecs extends AnyWordSpec with Matchers:

  import GameErrorCodecs.given

  "GameErrorCodecs" should:
    "encode and decode GameError.NotYourTurn correctly" in:
      val error: GameError = GameError.NotYourTurn
      val jsonString = error.toJson
      jsonString shouldBe """{"error":"NotYourTurn"}"""
      jsonString.decodeAs[GameError].value shouldBe error

    "encode and decode GameError.InvalidBid correctly" in:
      val error: GameError = GameError.InvalidBid
      val jsonString = error.toJson
      jsonString shouldBe """{"error":"InvalidBid"}"""
      jsonString.decodeAs[GameError].value shouldBe error

    "encode and decode GameError.InvalidAction correctly" in:
      val error: GameError = GameError.InvalidAction
      val jsonString = error.toJson
      jsonString shouldBe """{"error":"InvalidAction"}"""
      jsonString.decodeAs[GameError].value shouldBe error

    "encode and decode GameError.CardNotAllowed (CardNotInHand) correctly" in:
      import cards.Card.*
      val error: GameError =
        GameError.CardNotAllowed(CardNotAllowedReasons.CardNotInHand(List(Ten of Blue)))
      val jsonString = error.toJson
      jsonString shouldBe """{"error":"CardNotAllowed","reason":{"type":"CardNotInHand","legalCards":[{"type":"Standard","color":"Blue","rank":10}]}}"""
      jsonString.decodeAs[GameError].value shouldBe error

    "encode and decode GameError.CardNotAllowed (MustFollowColor) correctly" in:
      import cards.Card.*
      val error: GameError =
        GameError.CardNotAllowed(CardNotAllowedReasons.MustFollowColor(Red, List(Ten of Blue)))
      val jsonString = error.toJson
      jsonString shouldBe """{"error":"CardNotAllowed","reason":{"type":"MustFollowColor","requiredColor":"Red","legalCards":[{"type":"Standard","color":"Blue","rank":10}]}}"""
      jsonString.decodeAs[GameError].value shouldBe error
