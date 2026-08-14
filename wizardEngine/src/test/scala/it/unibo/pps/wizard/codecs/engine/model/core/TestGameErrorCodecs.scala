package it.unibo.pps.wizard.codecs.engine.model.core

import it.unibo.pps.wizard.codecs.syntax.CodecSyntax._
import it.unibo.pps.wizard.engine.model.basic._
import it.unibo.pps.wizard.engine.model.core._
import org.scalatest.EitherValues._
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

    "encode and decode GameError.InconsistentState (TableNoWinner) correctly" in:
      val error: GameError = GameError.InconsistentState(InconsistentStateReasons.TableNoWinner)
      val jsonString = error.toJson
      jsonString shouldBe """{"error":"InconsistentState","reason":{"type":"TableNoWinner"}}"""
      jsonString.decodeAs[GameError].value shouldBe error

    "encode and decode GameError.InconsistentState (HandNotFoundFor) correctly" in:
      val error: GameError =
        GameError.InconsistentState(InconsistentStateReasons.HandNotFoundFor(PlayerId(1)))
      val jsonString = error.toJson
      jsonString shouldBe """{"error":"InconsistentState","reason":{"type":"HandNotFoundFor","playerId":1}}"""
      jsonString.decodeAs[GameError].value shouldBe error
