package it.unibo.pps.wizard.engine.model.core

import it.unibo.pps.wizard.engine.events.ActionEvent
import it.unibo.pps.wizard.engine.events.LifecycleEvent
import it.unibo.pps.wizard.engine.events.ProgressEvent
import it.unibo.pps.wizard.engine.model.basic.BasicTestDSL._
import it.unibo.pps.wizard.engine.model.basic._
import it.unibo.pps.wizard.engine.model.basic.bidding.Bids
import it.unibo.pps.wizard.engine.model.basic.bidding.Tricks
import it.unibo.pps.wizard.engine.model.basic.cards.Card._
import it.unibo.pps.wizard.engine.model.basic.gameplay.Round
import it.unibo.pps.wizard.engine.model.basic.gameplay.Table
import it.unibo.pps.wizard.engine.model.core.GameError._
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import scala.language.postfixOps

class TestGameEngine extends AnyWordSpec with Matchers:

  val p1: Player = Player.human(PlayerId(1), PlayerName("Alice"))
  val p2: Player = Player.human(PlayerId(2), PlayerName("Bob"))
  val p3: Player = Player.human(PlayerId(3), PlayerName("Charlie"))
  val p4: Player = Player.human(PlayerId(4), PlayerName("David"))

  val mockPlayers: Players = Players(p1, p2, p3, p4)

  def createMockCore(roundValue: Int): CoreState =
    val round = Round(roundValue)
    CoreState.initialize(mockPlayers.getPlayerIds, round)

  "A GameEngine" should:
    "initialize a new game correctly via initializeGame" in:
      val engine = GameEngine.initializeGame(mockPlayers.getPlayerIds)

      engine.state match
        case _: GameState.Bidding       =>
        case _: GameState.ChoosingTrump =>
        case _ => fail("Expected GameState.Bidding or GameState.ChoosingTrump")

      engine.events.exists(_.isInstanceOf[ProgressEvent.CardsDealt]).shouldBe(true)
      engine.events.exists(_.isInstanceOf[ProgressEvent.PhaseChanged]).shouldBe(true)

    "trigger completeRound and nextRoundOrEnd when the last trick of a round is played" in:
      val c0 = Two of Blue
      val c1 = Ten of Blue
      val c2 = Four of Red
      val c3 = Five of Blue

      val hands = handsOf(
        p4.id holds c3
      )

      val core = createMockCore(1).copy(hands = hands)
      val currentTable = Table.empty + (p1.id -> c0) + (p2.id -> c1) + (p3.id -> c2)

      val playingState = GameState.Playing(
        core = core,
        bids = Bids.empty,
        table = currentTable,
        currentPlayerTurn = p4.id,
        tricksWon = Tricks.empty
      )

      val action = GameAction.PlayCard(p4.id, c3)
      val result = GameEngine.processAction(playingState, action)

      result.isRight.shouldBe(true)
      result.foreach: engine =>
        engine.state match
          case bidding: GameState.Bidding =>
            bidding.core.round.value shouldBe 2
          case choosing: GameState.ChoosingTrump =>
            choosing.core.round.value shouldBe 2
          case _ => fail("Expected transition to Round 2 (Bidding or ChoosingTrump)")

        engine.events.exists(_.isInstanceOf[ProgressEvent.RoundScored]).shouldBe(true)
        engine.events.exists(_.isInstanceOf[ProgressEvent.CardsDealt]).shouldBe(true)

    "trigger nextRoundOrEnd to end the game when the last trick of the final round is played" in:
      val lastRound = 60 / mockPlayers.totalPlayers
      val c0 = Two of Blue
      val c1 = Ten of Blue
      val c2 = Four of Red
      val c3 = Five of Blue

      val hands = handsOf(
        p4.id holds c3
      )

      val core = createMockCore(lastRound).copy(hands = hands)
      val currentTable = Table.empty + (p1.id -> c0) + (p2.id -> c1) + (p3.id -> c2)

      val playingState = GameState.Playing(
        core = core,
        bids = Bids.empty,
        table = currentTable,
        currentPlayerTurn = p4.id,
        tricksWon = Tricks.empty
      )

      val action = GameAction.PlayCard(p4.id, c3)
      val result = GameEngine.processAction(playingState, action)

      result.isRight shouldBe true
      result.foreach: engine =>
        engine.state match
          case _: GameState.Ended =>
          case _                  => fail("Expected GameState.Ended")

        engine.events.exists(_.isInstanceOf[ProgressEvent.RoundScored]).shouldBe(true)
        engine.events.exists(_.isInstanceOf[LifecycleEvent.GameEnded]).shouldBe(true)

    "allow resolving an unresolved wizard trump during ChoosingTrump phase" in:
      val core = createMockCore(1).updateTrump(Option(wizard).asTrump)
      val choosingState = GameState.ChoosingTrump(core)
      val action = GameAction.ResolveTrumpColor(p1.id, Color.Red)

      val result = GameEngine.processAction(choosingState, action)

      result.isRight.shouldBe(true)
      result.foreach: engine =>
        engine.state match
          case nextState: GameState.Bidding =>
            nextState.core.trump.effectiveColor shouldBe Some(Color.Red)
          case _ => fail("Expected GameState.Bidding")
        engine.events should contain(ActionEvent.TrumpColorResolved(p1.id, Color.Red))

    "allow the current player to place a valid bid" in:
      val core = createMockCore(1)
      val biddingState = GameState.Bidding(core, Bids.empty, p1.id)
      val action = GameAction.PlaceBid(p1.id, 1)

      val result = GameEngine.processAction(biddingState, action)

      result.isRight.shouldBe(true)
      result.foreach: engine =>
        engine.state match
          case nextState: GameState.Bidding =>
            nextState.currentBids(p1.id) shouldBe 1
            nextState.currentPlayer shouldBe p2.id
          case _ => fail("Expected GameState.Bidding")

        engine.events should contain(ActionEvent.BidPlaced(p1.id, 1))

    "fail with NotYourTurn when a player places a bid out of turn" in:
      val core = createMockCore(1)
      val biddingState = GameState.Bidding(core, Bids.empty, p1.id)
      val action = GameAction.PlaceBid(p2.id, 3)

      val result = GameEngine.processAction(biddingState, action)

      result shouldBe Left(NotYourTurn)

    "transition from Bidding to Playing phase when the last player places their bid" in:
      val hands = handsOf(
        p1.id holds (Five of Red)
      )
      val core = createMockCore(1).copy(hands = hands)

      val currentBids = Bids.empty + (p1.id -> 0) + (p2.id -> 1) + (p3.id -> 0)
      val biddingState = GameState.Bidding(core, currentBids, p4.id)

      val action = GameAction.PlaceBid(p4.id, 1)
      val result = GameEngine.processAction(biddingState, action)

      result.isRight.shouldBe(true)
      result.foreach: engine =>
        engine.state match
          case playingState: GameState.Playing =>
            playingState.table.playedCards.isEmpty.shouldBe(true)
            playingState.currentPlayerTurn shouldBe p1.id
          case _ => fail("Expected GameState.Playing")

        engine.events.exists(_.isInstanceOf[ProgressEvent.PhaseChanged]).shouldBe(true)

    "allow playing a card, removing it from hand and adding it to the table" in:
      val c1 = Five of Blue
      val hands = handsOf(
        p1.id holds c1,
        p2.id holds c1
      )
      val core = createMockCore(1).copy(hands = hands)

      val playingState = GameState.Playing(
        core = core,
        bids = Bids.empty,
        table = Table.empty,
        currentPlayerTurn = p1.id,
        tricksWon = Tricks.empty
      )

      val action = GameAction.PlayCard(p1.id, c1)
      val result = GameEngine.processAction(playingState, action)

      result.isRight.shouldBe(true)
      result.foreach: engine =>
        engine.state match
          case nextState: GameState.Playing =>
            nextState.table.playedCards should contain(c1)
            nextState.currentPlayerTurn shouldBe p2.id
          case _ => fail("Expected GameState.Playing")

    "evaluate the trick winner and reset the table when the trick is complete" in:
      val c0 = Two of Blue
      val c1 = Ten of Blue
      val c2 = Four of Red
      val c3 = Five of Blue

      val extraCard = Three of Yellow
      val hands = handsOf(
        p1.id holds extraCard,
        p2.id holds extraCard,
        p3.id holds extraCard,
        p4.id holds (c3 - extraCard)
      )

      val core = createMockCore(2).copy(hands = hands)
      val currentTable = Table.empty + (p1.id -> c0) + (p2.id -> c1) + (p3.id -> c2)

      val playingState = GameState.Playing(
        core = core,
        bids = Bids.empty,
        table = currentTable,
        currentPlayerTurn = p4.id,
        tricksWon = Tricks.empty
      )

      val action = GameAction.PlayCard(p4.id, c3)
      val result = GameEngine.processAction(playingState, action)

      result.isRight.shouldBe(true)
      result.foreach: engine =>
        engine.state match
          case nextState: GameState.Playing =>
            nextState.table.playedCards.isEmpty.shouldBe(true)
            nextState.tricksWon(p2.id) shouldBe 1
            nextState.currentPlayerTurn shouldBe p2.id
          case _ => fail("Expected GameState.Playing")

    "fail with InvalidAction when does not match the current game state" in:
      val core = createMockCore(1)
      val choosingState = GameState.ChoosingTrump(core)
      val action = GameAction.PlaceBid(p1.id, 1)

      val result = GameEngine.processAction(choosingState, action)

      result shouldBe Left(InvalidAction)
