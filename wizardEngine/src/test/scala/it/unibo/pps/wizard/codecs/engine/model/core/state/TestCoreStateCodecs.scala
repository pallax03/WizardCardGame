package it.unibo.pps.wizard.codecs.engine.model.core.state

import it.unibo.pps.wizard.codecs.syntax.CodecSyntax._
import it.unibo.pps.wizard.engine.model._
import it.unibo.pps.wizard.engine.model.core.state.PlayerCoreState
import it.unibo.pps.wizard.engine.model.core.state.ServerCoreState
import org.scalatest.EitherValues._
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class TestCoreStateCodecs extends AnyWordSpec with Matchers:

  import CoreStateCodecs.given
  import basic.BasicTestDSL.*
  import basic.{PlayerId, Scoreboard}
  import basic.cards.Card.*
  import basic.gameplay.Round

  "CoreStateCodecs" should:
    val p1 = PlayerId(1)
    val c1 = Ten of Blue
    val c2 = One of Red
    val p2 = PlayerId(2)
    val c3 = Two of Green
    val core: ServerCoreState = ServerCoreState
      .initialize(
        List(p1, p2),
        Round.start
      )
      .copy(
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
    "encode and decode ServerCoreState correctly" in:
      val jsonString = core.toJson
      jsonString shouldBe """{"playersIds":[1,2],"hands":{"1":[{"type":"Standard","color":"Blue","rank":10},{"type":"Standard","color":"Red","rank":1}],"2":[{"type":"Standard","color":"Green","rank":2}]},"trump":{"type":"Absent"},"round":2,"dealerId":2,"scoreboard":{"1":[{"round":1,"score":20,"bid":0}],"2":[{"round":1,"score":-10,"bid":1}]}}"""
      jsonString.decodeAs[ServerCoreState].value shouldBe core
    "encode and decode PlayerCoreState correctly" in:
      val jsonString = PlayerCoreState.from(core, p1).toOption.get.toJson
      println(jsonString)
//      jsonString shouldBe """"""
      jsonString
        .decodeAs[PlayerCoreState]
        .value shouldBe PlayerCoreState.from(core, p1).toOption.get
