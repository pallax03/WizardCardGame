package io.github.pallax03.wizard.engine.model.core

import scala.language.postfixOps

import io.github.pallax03.wizard.engine.model.basic._
import io.github.pallax03.wizard.engine.model.core.GameError._
import io.github.pallax03.wizard.engine.model.core.state.{GameState, ServerCoreState}
import io.github.pallax03.wizard.engine.model.events.{ActionEvent, LifecycleEvent, ProgressEvent}

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class TestGameEngine extends AnyWordSpec with Matchers:

  import BasicTestDSL._
  import bidding._
  import cards.Card._
  import gameplay.Round
  import gameplay.Table

  val p1: PlayerId = PlayerId(1)
  val p2: PlayerId = PlayerId(2)
  val p3: PlayerId = PlayerId(3)
  val p4: PlayerId = PlayerId(4)

  val mockPlayersIds: List[PlayerId] = List(p1, p2, p3, p4)

  def createMockCore(round: Round): ServerCoreState =
    ServerCoreState.initialize(mockPlayersIds, round)

  "A GameEngine" should:
    "initialize a new game correctly via initializeGame" in:
      val engine = GameEngine.initializeGame(mockPlayersIds)

      engine.state match
        case _: GameState.Bidding[?]       =>
        case _: GameState.ChoosingTrump[?] =>
        case _ => fail("Expected GameState.Bidding or GameState.ChoosingTrump")

      engine.events.exists(_.isInstanceOf[ProgressEvent.CardsDealt]) shouldBe true
      engine.events.exists(_.isInstanceOf[ProgressEvent.PhaseChanged]) shouldBe true

    "trigger completeRound and nextRoundOrEnd when the last trick of a round is played" in:
      val c0 = Two of Blue
      val c1 = Ten of Blue
      val c2 = Four of Red
      val c3 = Five of Blue

      val hands = handsOf(
        p4 holds c3
      )

      val core = createMockCore(1).copy(hands = hands)
      val currentTable = Table.empty + (p1 plays c0) + (p2 plays c1) + (p3 plays c2)

      val playingState = GameState.Playing(
        core = core,
        bids = Bids.empty,
        table = currentTable,
        playerTurn = p4,
        tricksWon = Tricks.empty
      )

      val action = GameAction.PlayCard(p4, c3)
      val result = GameEngine.processAction(playingState, action)

      result.isRight shouldBe true
      result.foreach: engine =>
        engine.state match
          case bidding: GameState.Bidding[?] =>
            bidding.core.round shouldBe 2
          case choosing: GameState.ChoosingTrump[?] =>
            choosing.core.round shouldBe 2
          case _ => fail("Expected transition to Round 2 (Bidding or ChoosingTrump)")

        engine.events.exists(_.isInstanceOf[ProgressEvent.RoundScored]) shouldBe true
        engine.events.exists(_.isInstanceOf[ProgressEvent.CardsDealt]) shouldBe true

    "trigger nextRoundOrEnd to end the game when the last trick of the final round is played" in:
      val lastRound = 60 / mockPlayersIds.length
      val c0 = Two of Blue
      val c1 = Ten of Blue
      val c2 = Four of Red
      val c3 = Five of Blue

      val hands = handsOf(
        p4 holds c3
      )

      val core = createMockCore(lastRound).copy(hands = hands)
      val currentTable = Table.empty + (p1 plays c0) + (p2 plays c1) + (p3 plays c2)

      val playingState = GameState.Playing(
        core = core,
        bids = Bids.empty,
        table = currentTable,
        playerTurn = p4,
        tricksWon = Tricks.empty
      )

      val action = GameAction.PlayCard(p4, c3)
      val result = GameEngine.processAction(playingState, action)

      result.isRight shouldBe true
      result.foreach: engine =>
        engine.state match
          case _: GameState.Ended =>
          case _                  => fail("Expected GameState.Ended")

        engine.events.exists(_.isInstanceOf[ProgressEvent.RoundScored]) shouldBe true
        engine.events.exists(_.isInstanceOf[LifecycleEvent.GameEnded]) shouldBe true

    "allow resolving an unresolved wizard trump during ChoosingTrump phase" in:
      val core = createMockCore(1).updateTrump(Option(wizard).asTrump)
      val choosingState = GameState.ChoosingTrump(core)
      val action = GameAction.ResolveTrumpColor(p1, Color.Red)

      val result = GameEngine.processAction(choosingState, action)

      result.isRight shouldBe true
      result.foreach: engine =>
        engine.state match
          case nextState: GameState.Bidding[?] =>
            nextState.core.trump.effectiveColor shouldBe Some(Color.Red)
          case _ => fail("Expected GameState.Bidding")
        engine.events should contain(ActionEvent.TrumpColorResolved(p1, Color.Red))

    "allow the current player to place a valid bid" in:
      val core = createMockCore(1)
      val biddingState = GameState.Bidding(core, Bids.empty, p1)
      val action = GameAction.PlaceBid(p1, 1)

      val result = GameEngine.processAction(biddingState, action)

      result.isRight shouldBe true
      result.foreach: engine =>
        engine.state match
          case nextState: GameState.Bidding[?] =>
            nextState.playerTurn shouldBe p2
            nextState.bids(p1) shouldBe 1
          case _ => fail("Expected GameState.Bidding")

        engine.events should contain(ActionEvent.BidPlaced(p1, 1))

    "fail with NotYourTurn when a player places a bid out of turn" in:
      val core = createMockCore(1)
      val biddingState = GameState.Bidding(core, Bids.empty, p1)
      val action = GameAction.PlaceBid(p2, 3)

      val result = GameEngine.processAction(biddingState, action)

      result shouldBe Left(NotYourTurn)

    "transition from Bidding to Playing phase when the last player places their bid" in:
      val hands = handsOf(
        p1 holds (Five of Red)
      )
      val core = createMockCore(1).copy(hands = hands)

      val currentBids = Bids.empty + (p1 place 0) + (p2 place 1) + (p3 place 0)
      val biddingState = GameState.Bidding(core, currentBids, p4)

      val action = GameAction.PlaceBid(p4, 1)
      val result = GameEngine.processAction(biddingState, action)

      result.isRight shouldBe true
      result.foreach: engine =>
        engine.state match
          case playingState: GameState.Playing[?] =>
            playingState.table.playedCards.isEmpty shouldBe true
            playingState.playerTurn shouldBe p1
          case _ => fail("Expected GameState.Playing")

        engine.events.exists(_.isInstanceOf[ProgressEvent.PhaseChanged]) shouldBe true

    "allow playing a card, removing it from hand and adding it to the table" in:
      val c1 = Five of Blue
      val hands = handsOf(
        p1 holds c1,
        p2 holds c1
      )
      val core = createMockCore(1).copy(hands = hands)

      val playingState = GameState.Playing(
        core = core,
        bids = Bids.empty,
        table = Table.empty,
        playerTurn = p1,
        tricksWon = Tricks.empty
      )

      val action = GameAction.PlayCard(p1, c1)
      val result = GameEngine.processAction(playingState, action)

      result.isRight shouldBe true
      result.foreach: engine =>
        engine.state match
          case nextState: GameState.Playing[?] =>
            nextState.table.playedCards should contain(c1)
            nextState.playerTurn shouldBe p2
          case _ => fail("Expected GameState.Playing")

    "evaluate the trick winner and reset the table when the trick is complete" in:
      val c0 = Two of Blue
      val c1 = Ten of Blue
      val c2 = Four of Red
      val c3 = Five of Blue

      val extraCard = Three of Yellow
      val hands = handsOf(
        p1 holds extraCard,
        p2 holds extraCard,
        p3 holds extraCard,
        p4 holds (c3 - extraCard)
      )

      val core = createMockCore(2).copy(hands = hands)
      val currentTable = Table.empty + (p1 plays c0) + (p2 plays c1) + (p3 plays c2)

      val playingState = GameState.Playing(
        core = core,
        bids = Bids.empty,
        table = currentTable,
        playerTurn = p4,
        tricksWon = Tricks.empty
      )

      val action = GameAction.PlayCard(p4, c3)
      val result = GameEngine.processAction(playingState, action)

      result.isRight shouldBe true
      result.foreach: engine =>
        engine.state match
          case nextState: GameState.Playing[?] =>
            nextState.table.playedCards.isEmpty shouldBe true
            nextState.tricksWon(p2) shouldBe 1
            nextState.playerTurn shouldBe p2
          case _ => fail("Expected GameState.Playing")

    "fail with InvalidAction when does not match the current game state" in:
      val core = createMockCore(1)
      val choosingState = GameState.ChoosingTrump(core)
      val action = GameAction.PlaceBid(p1, 1)

      val result = GameEngine.processAction(choosingState, action)

      result shouldBe Left(InvalidAction)
