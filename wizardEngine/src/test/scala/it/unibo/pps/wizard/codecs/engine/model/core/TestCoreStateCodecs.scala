package it.unibo.pps.wizard.codecs.engine.model.core

import it.unibo.pps.wizard.codecs.syntax.CodecSyntax.*
import it.unibo.pps.wizard.engine.model.*
import it.unibo.pps.wizard.engine.model.core.state.ServerCoreState
import org.scalatest.EitherValues.*
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class TestCoreStateCodecs extends AnyWordSpec with Matchers:

  import basic.PlayerId
  import basic.cards.Card.*
  import basic.gameplay.Round
  import basic.Scoreboard
  import basic.BasicTestDSL.*
  import CoreStateCodecs.given

  "CoreStateCodecs" should:
    val p1 = PlayerId(1)
    val c1 = Ten of Blue
    val c2 = One of Red
    val p2 = PlayerId(2)
    val c3 = Two of Green
    "encode and decode ServerCoreState correctly" in:
      val core: ServerCoreState = ServerCoreState.initialize(
        List(p1, p2),
        Round.start
      ).copy(
        round = 2,
        dealerId = p2,
        hands = handsOf(
          p1 holds (c1 - c2),
          p2 holds c3
          ),
        scoreboard = Scoreboard.empty
          .addScore(p1, 1, 20, 0)
          .addScore(p2, 1, -10, 1)
      )
      val jsonString = core.toJson
      jsonString shouldBe """{"playersIds":[1,2],"hands":{"1":[{"type":"Standard","color":"Blue","rank":10},{"type":"Standard","color":"Red","rank":1}],"2":[{"type":"Standard","color":"Green","rank":2}]},"trump":{"type":"Absent"},"round":2,"dealerId":2,"scoreboard":{"1":[{"round":1,"score":20,"bid":0}],"2":[{"round":1,"score":-10,"bid":1}]}}"""
      jsonString.decodeAs[ServerCoreState].value shouldBe core
