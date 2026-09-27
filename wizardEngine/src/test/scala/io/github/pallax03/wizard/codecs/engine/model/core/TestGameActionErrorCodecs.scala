package io.github.pallax03.wizard.codecs.engine.model.core

import io.github.pallax03.wizard.codecs.syntax.CodecSyntax.*
import io.github.pallax03.wizard.engine.model.basic.*
import io.github.pallax03.wizard.engine.model.core.*

import org.scalatest.EitherValues.*
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class TestGameActionErrorCodecs extends AnyWordSpec with Matchers:

  import GameActionErrorCodecs.given

  "GameActionErrorCodecs" should:
    "encode and decode GameActionError.NotYourTurn correctly" in:
      val error: GameActionError = GameActionError.NotYourTurn(PlayerId(2))
      val jsonString = error.toJson
      jsonString shouldBe """{"error":"NotYourTurn","turnOf":2}"""
      jsonString.decodeAs[GameActionError].value shouldBe error

    "encode and decode GameActionError.InvalidBid correctly" in:
      val error: GameActionError = GameActionError.InvalidBid(5, 3)
      val jsonString = error.toJson
      jsonString shouldBe """{"error":"InvalidBid","round":5,"bid":3}"""
      jsonString.decodeAs[GameActionError].value shouldBe error

    "encode and decode GameActionError.InvalidAction correctly" in:
      val error: GameActionError = GameActionError.InvalidAction(None)
      val jsonString = error.toJson
      jsonString shouldBe """{"error":"InvalidAction","invitationEvent":null}"""
      jsonString.decodeAs[GameActionError].value shouldBe error

    "encode and decode GameActionError.CardNotAllowed (CardNotInHand) correctly" in:
      import cards.Card.*
      val error: GameActionError =
        GameActionError.CardNotAllowed(CardNotAllowedReasons.CardNotInHand(List(Ten of Blue)))
      val jsonString = error.toJson
      jsonString shouldBe """{"error":"CardNotAllowed","reason":{"type":"CardNotInHand","legalCards":[{"type":"Standard","color":"Blue","rank":10}]}}"""
      jsonString.decodeAs[GameActionError].value shouldBe error

    "encode and decode GameActionError.CardNotAllowed (MustFollowColor) correctly" in:
      import cards.Card.*
      val error: GameActionError =
        GameActionError.CardNotAllowed(CardNotAllowedReasons.MustFollowColor(Red, List(Ten of Blue)))
      val jsonString = error.toJson
      jsonString shouldBe """{"error":"CardNotAllowed","reason":{"type":"MustFollowColor","requiredColor":"Red","legalCards":[{"type":"Standard","color":"Blue","rank":10}]}}"""
      jsonString.decodeAs[GameActionError].value shouldBe error
