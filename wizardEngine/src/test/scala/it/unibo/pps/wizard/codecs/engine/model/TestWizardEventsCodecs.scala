package it.unibo.pps.wizard.codecs.engine.model

import it.unibo.pps.wizard.codecs.syntax.CodecSyntax.*
import it.unibo.pps.wizard.engine.model.*
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class TestWizardEventsCodecs extends AnyWordSpec with Matchers:

  import basic._
  import basic.cards.Card.*
  import events._
  import core.GameError
  import basic.BasicTestDSL.*
  import WizardEventsCodecs.given

  "encode WizardEvents" when:
    val p1 = PlayerId(1)
    val p2 = PlayerId(2)
    val playersIds = List(p1, p2)
    val round = 2
    val p1Cards = (One of Green) - (Five of Yellow)
    val hands = handsOf(
      p1 holds p1Cards,
      p2 holds (Thirteen of Red)
    )
    val playedCard = Eight of Yellow
    val trump = Option(Ten of Blue).asTrump
    val scoreboard = Scoreboard.empty.addScore(p1, 1, 20, 0).addScore(p2, 1, -10, 0)
    "LifecycleEvent" should:
      "encode GameStarted correctly" in:
        val event: WizardEvent = LifecycleEvent.GameStarted(playersIds)
        val jsonString = event.toJson
        jsonString shouldBe """{"event":{"type":"LifecycleEvent","action":"GameStarted","fields":{"playersIds":[1,2]}}}"""
      "encode GameEnded correctly" in :
        val event: WizardEvent = LifecycleEvent.GameEnded(playersIds, scoreboard)
        val jsonString = event.toJson
        jsonString shouldBe """{"event":{"type":"LifecycleEvent","action":"GameEnded","fields":{"playersIds":[1,2],"finalScores":{"1":[{"round":1,"score":20,"bid":0}],"2":[{"round":1,"score":-10,"bid":0}]}}}}"""

    "ProgressEvent" should:
      "encode CardsDealt correctly" in:
        val event: WizardEvent = ProgressEvent.CardsDealt(p1, hands, trump, round)
        val jsonString = event.toJson
        jsonString shouldBe """{"event":{"type":"ProgressEvent","action":"CardsDealt","destinationId":1,"fields":{"playerId":1,"hands":{"1":[{"type":"Standard","color":"Green","rank":1},{"type":"Standard","color":"Yellow","rank":5}],"2":[{"type":"Standard","color":"Red","rank":13}]},"trump":{"type":"Standard","card":{"type":"Standard","color":"Blue","rank":10},"color":"Blue"},"round":2}}}"""
      "encode TrickWon correctly" in:
        val event: WizardEvent = ProgressEvent.TrickWon(p1, 0, p1Cards)
        val jsonString = event.toJson
        jsonString shouldBe """{"event":{"type":"ProgressEvent","action":"TrickWon","playerId":1,"fields":{"winnerId":1,"tricksWon":0,"trickedCards":[{"type":"Standard","color":"Green","rank":1},{"type":"Standard","color":"Yellow","rank":5}]}}}"""
      "encode RoundScored correctly" in:
        val event: WizardEvent = ProgressEvent.RoundScored(playersIds, Scoreboard.empty)
        val jsonString = event.toJson
        jsonString shouldBe """{"event":{"type":"ProgressEvent","action":"RoundScored","fields":{"playersIds":[1,2],"scoreboard":{}}}}"""
      "encode PhaseChanged correctly" in:
        val event: WizardEvent = ProgressEvent.PhaseChanged("Bidding")
        val jsonString = event.toJson
        jsonString shouldBe """{"event":{"type":"ProgressEvent","action":"PhaseChanged","fields":{"phaseName":"Bidding"}}}"""
    "ActionEvent" should:
      "encode TrumpColorResolved correctly" in:
        val event: WizardEvent = ActionEvent.TrumpColorResolved(p1, Color.Red)
        val jsonString = event.toJson
        jsonString shouldBe """{"event":{"type":"ActionEvent","action":"TrumpColorResolved","playerId":1,"fields":{"playerId":1,"color":"Red"}}}"""
      "encode BidPlaced correctly" in:
        val event: WizardEvent = ActionEvent.BidPlaced(p1, 1)
        val jsonString = event.toJson
        jsonString shouldBe """{"event":{"type":"ActionEvent","action":"BidPlaced","playerId":1,"fields":{"playerId":1,"bid":1}}}"""
      "encode CardPlayed correctly" in:
        val event: WizardEvent = ActionEvent.CardPlayed(p2, playedCard, Option(playedCard), Option(Color.Red))
        val jsonString = event.toJson
        jsonString shouldBe """{"event":{"type":"ActionEvent","action":"CardPlayed","playerId":2,"fields":{"playerId":2,"card":{"type":"Standard","color":"Yellow","rank":8},"winningCard":{"type":"Standard","color":"Yellow","rank":8},"followingColor":"Red"}}}"""
    "InvitationEvent" should:
      "encode WaitingForTrump correctly" in :
        val event: WizardEvent = InvitationEvent.WaitingForTrump(p1)
        val jsonString = event.toJson
        jsonString shouldBe """{"event":{"type":"InvitationEvent","action":"WaitingForTrump","playerId":1,"fields":{"playerId":1}}}"""
    "encode WaitingForBid correctly" in :
      val event: WizardEvent = InvitationEvent.WaitingForBid(p2, round)
      val jsonString = event.toJson
      jsonString shouldBe """{"event":{"type":"InvitationEvent","action":"WaitingForBid","playerId":2,"fields":{"playerId":2,"round":2}}}"""
    "encode WaitingForCard correctly" in :
      val event: WizardEvent = InvitationEvent.WaitingForCard(p1, p1Cards)
      val jsonString = event.toJson
      jsonString shouldBe """{"event":{"type":"InvitationEvent","action":"WaitingForCard","playerId":1,"fields":{"playerId":1,"legalCards":[{"type":"Standard","color":"Green","rank":1},{"type":"Standard","color":"Yellow","rank":5}]}}}"""
    "FailureEvent" should:
      "encode ActionFailed correctly" in:
        val event: WizardEvent = FailureEvent.ActionFailed(p1, GameError.InvalidAction)
        val jsonString = event.toJson
        jsonString shouldBe """{"event":{"type":"FailureEvent","action":"ActionFailed","destinationId":1,"fields":{"playerId":1,"reason":{"error":"InvalidAction"}}}}"""