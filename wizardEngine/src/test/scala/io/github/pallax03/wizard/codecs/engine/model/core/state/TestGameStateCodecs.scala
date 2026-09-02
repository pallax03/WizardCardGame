package io.github.pallax03.wizard.codecs.engine.model.core.state

import io.github.pallax03.wizard.codecs.syntax.CodecSyntax._
import io.github.pallax03.wizard.engine.model._
import io.github.pallax03.wizard.engine.model.basic.gameplay.Trump.WizardUnresolved
import io.github.pallax03.wizard.engine.model.core.state.{
  GameState,
  PlayerGameState,
  ServerCoreState
}

import org.scalatest.EitherValues._
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class TestGameStateCodecs extends AnyWordSpec with Matchers:

  import basic.BasicTestDSL.*
  import GameStateCodecs.given
  import basic.*
  import basic.bidding.*
  import basic.cards.Card.*
  import basic.gameplay.*
  "GameStateCodecs" should:
    val p1 = PlayerId(1)
    val p2 = PlayerId(2)
    val core = ServerCoreState.initialize(
      List(p1, p2),
      Round.start
    )
    val coreWithHands = core.copy(hands =
      handsOf(
        p1 holds (Ten of Blue),
        p2 holds (Thirteen of Yellow)
      )
    )
    "encode and decode GameState.ChoosingTrump correctly" in:
      val serverState = GameState.ChoosingTrump(core.updateTrump(WizardUnresolved(wizard)))
      val serverStateJsonString = serverState.toJson
      serverStateJsonString should fullyMatch regex """\{"core":\{"playersIds":\[1,2\],"hands":\{\},"trump":\{"type":"WizardUnresolved","card":\{"type":"Wizard","id":[0-3]\}\},"round":1,"dealerId":1,"scoreboard":\{\}\}\}"""
      serverStateJsonString
        .decodeAs[GameState.ChoosingTrump[ServerCoreState]]
        .value shouldBe serverState
    "encode and decode GameState.Bidding correctly" in:
      val serverState = GameState.Bidding(
        coreWithHands,
        bids = Bids.empty + (p1 place 1) + (p2 place 1),
        playerTurn = p1
      )
      val serverStateJsonString = serverState.toJson
      serverStateJsonString shouldBe """{"core":{"playersIds":[1,2],"hands":{"1":[{"type":"Standard","color":"Blue","rank":10}],"2":[{"type":"Standard","color":"Yellow","rank":13}]},"trump":{"type":"Absent"},"round":1,"dealerId":1,"scoreboard":{}},"bids":{"1":1,"2":1},"playerTurn":1}"""
      serverStateJsonString.decodeAs[GameState.Bidding[ServerCoreState]].value shouldBe serverState
    "encode and decode GameState.Playing correctly" in:
      val serverState = GameState.Playing(
        coreWithHands,
        bids = Bids.empty + (p1 place 1) + (p2 place 1),
        table = Table.empty + (p1 plays (Ten of Blue)),
        playerTurn = p2,
        tricksWon = Tricks.empty.addTrickTo(p1)
      )
      val serverStateJsonString = serverState.toJson
      serverStateJsonString shouldBe """{"core":{"playersIds":[1,2],"hands":{"1":[{"type":"Standard","color":"Blue","rank":10}],"2":[{"type":"Standard","color":"Yellow","rank":13}]},"trump":{"type":"Absent"},"round":1,"dealerId":1,"scoreboard":{}},"bids":{"1":1,"2":1},"table":{"playedCards":[{"playerId":1,"card":{"type":"Standard","color":"Blue","rank":10}}],"followingColor":"Blue"},"playerTurn":2,"tricksWon":{"1":1}}"""
      serverStateJsonString.decodeAs[GameState.Playing[ServerCoreState]].value shouldBe serverState
      val playerStateJsonString = PlayerGameState.from(serverState, p1).toJson
      playerStateJsonString shouldBe """{"Playing":{"core":{"playersIds":[1,2],"hand":[{"type":"Standard","color":"Blue","rank":10}],"trump":{"type":"Absent"},"round":1,"dealerId":1,"scoreboard":{}},"bids":{"1":1,"2":1},"table":{"playedCards":[{"playerId":1,"card":{"type":"Standard","color":"Blue","rank":10}}],"followingColor":"Blue"},"playerTurn":2,"tricksWon":{"1":1},"currentWinner":1}}"""
    "encode and decode GameState.Ended correctly" in:
      val serverState = GameState.Ended(
        playersIds = core.playersIds,
        scoreboard = Scoreboard.empty
          .addScore(p1, 1, 20, 0)
          .addScore(p2, 1, -10, 1)
      )
      val serverStateJsonString = serverState.toJson
      serverStateJsonString shouldBe """{"playersIds":[1,2],"scoreboard":{"1":[{"round":1,"score":20,"bid":0}],"2":[{"round":1,"score":-10,"bid":1}]}}"""
      serverStateJsonString.decodeAs[GameState.Ended].value shouldBe serverState
