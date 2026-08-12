package it.unibo.pps.wizard.codecs.engine.model.core

import it.unibo.pps.wizard.codecs.syntax.CodecSyntax.*
import it.unibo.pps.wizard.engine.model.*
import it.unibo.pps.wizard.engine.model.basic.gameplay.Trump.WizardUnresolved
import org.scalatest.EitherValues.*
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class TestGameStateCodecs extends AnyWordSpec with Matchers:

  import GameStateCodecs.given
  import core.{CoreState, GameState}
  import basic._
  import basic.gameplay._
  import basic.bidding._
  import basic.cards.Card.*
  import BasicTestDSL.*
  "GameStateCodecs" should:
    val p1 = PlayerId(1)
    val p2 = PlayerId(2)
    val core = CoreState.initialize(
      List(p1, p2),
      Round.start
    )
    "encode and decode GameState.ChoosingTrump correctly" in:
      val state = GameState.ChoosingTrump(core.updateTrump(WizardUnresolved(wizard)))
      val jsonString = state.toJson
      jsonString should fullyMatch regex """\{"core":\{"playersIds":\[1,2\],"hands":\{\},"trump":\{"type":"WizardUnresolved","card":\{"type":"Wizard","id":[0-3]\}\},"round":1,"dealerId":1,"scoreboard":\{\}\}\}"""
      jsonString.decodeAs[GameState.ChoosingTrump].value shouldBe state
    "encode and decode GameState.Bidding correctly" in :
      val state = GameState.Bidding(
        core,
        bids = Bids.empty + (p1 place 1) + (p2 place 1),
        playerTurn = p1
      )
      val jsonString = state.toJson
      jsonString shouldBe """{"core":{"playersIds":[1,2],"hands":{},"trump":{"type":"Absent"},"round":1,"dealerId":1,"scoreboard":{}},"bids":{"1":1,"2":1},"playerTurn":1}"""
      jsonString.decodeAs[GameState.Bidding].value shouldBe state
    "encode and decode GameState.Playing correctly" in :
      val state = GameState.Playing(
        core,
        bids = Bids.empty + (p1 place 1) + (p2 place 1),
        table = Table.empty + (p1 plays (Ten of Blue)),
        playerTurn = p2,
        tricksWon = Tricks.empty.addTrickTo(p1)
      )
      val jsonString = state.toJson
      jsonString shouldBe """{"core":{"playersIds":[1,2],"hands":{},"trump":{"type":"Absent"},"round":1,"dealerId":1,"scoreboard":{}},"bids":{"1":1,"2":1},"table":{"playedCards":[{"playerId":1,"card":{"type":"Standard","color":"Blue","rank":10}}],"followingColor":"Blue"},"playerTurn":2,"tricksWon":{"1":1}}"""
      jsonString.decodeAs[GameState.Playing].value shouldBe state
    "encode and decode GameState.Ended correctly" in :
      val state = GameState.Ended(
        playersIds = core.playersIds,
        scoreboard = Scoreboard.empty
          .addScore(p1, 1, 20, 0)
          .addScore(p2, 1, -10, 1)
      )
      val jsonString = state.toJson
      jsonString shouldBe """{"playersIds":[1,2],"scoreboard":{"1":[{"round":1,"score":20,"bid":0}],"2":[{"round":1,"score":-10,"bid":1}]}}"""
      jsonString.decodeAs[GameState.Ended].value shouldBe state
