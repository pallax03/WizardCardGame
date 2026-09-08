package io.github.pallax03.wizard.engine.model.core.state

import io.github.pallax03.wizard.engine.model.basic.*
import io.github.pallax03.wizard.engine.model.events.InvitationEvent

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class TestGameState extends AnyWordSpec with Matchers:
  import BasicTestDSL._
  import bidding.{Bids, Tricks}
  import cards.Card
  import gameplay.Table

  "GameState pendingInvitation" when:
    val p1 = PlayerId(1)
    val p2 = PlayerId(2)
    val players = List(p1, p2)
    val core = ServerCoreState.initialize(players, 1)

    "in ChoosingTrump phase" should:
      "return WaitingForTrump for the dealer" in:
        val state = GameState.ChoosingTrump(core)
        state.pendingInvitation(p1) shouldBe Some(InvitationEvent.WaitingForTrump(p1))
        state.pendingInvitation(p2) shouldBe None

    "in Bidding phase" should:
      "return WaitingForBid with the restricted bid if it's the last player" in:
        val bids = Bids.empty + (p1 place 1)
        GameState.Bidding(core, bids, p2).pendingInvitation(p2) match
          case Some(InvitationEvent.WaitingForBid(p, 1, Some(0))) =>
            p shouldBe p2
          case _ => fail("Expected WaitingForBid with restricted bid")

    "in Playing phase" should:
      "return WaitingForCard with legal cards" in:
        val cards = jester - (Five of Red)
        val hands = handsOf(
          p1 holds cards
        )
        GameState
          .Playing(core.copy(hands = hands), Bids.empty, Table.empty, p1, Tricks.empty)
          .pendingInvitation(p1) match
          case Some(InvitationEvent.WaitingForCard(p, hand)) =>
            p shouldBe p1
            hand shouldBe cards
          case _ => fail("Expected WaitingForCard")

    "in Ended phase" should:
      "return None" in:
        val state = GameState.Ended(players, Scoreboard.empty)
        state.pendingInvitation(p1) shouldBe None

  "PlayerGameState from" when:
    val p1 = PlayerId(1)
    val p2 = PlayerId(2)
    val players = List(p1, p2)
    val core = ServerCoreState
      .initialize(players, 1)
      .copy(hands =
        handsOf(
          p1 holds jester,
          p2 holds wizard
        )
      )

    "converting ServerGameState" should:
      "in ChoosingTrump" in:
        val serverState = GameState.ChoosingTrump(core)
        players.foreach: pId =>
          PlayerGameState.from(serverState, pId) match
            case GameState.ChoosingTrump(pCore) =>
              pCore.hand.toList shouldBe core.hands.getHand(pId).toList
            case _ => fail("Not valid Hand for " + pId)

      "in Bidding" in:
        val serverState = GameState.Bidding(core, Bids.empty, p1)
        players.foreach: pId =>
          PlayerGameState.from(serverState, pId) match
            case GameState.Bidding(pCore, _, _) =>
              pCore.hand.toList shouldBe core.hands.getHand(pId).toList
            case _ => fail("Not valid Hand for " + pId)

      "in Playing" in:
        val serverState = GameState.Playing(core, Bids.empty, Table.empty, p1, Tricks.empty)
        players.foreach: pId =>
          PlayerGameState.from(serverState, pId) match
            case GameState.Playing(pCore, _, _, _, _) =>
              pCore.hand.toList shouldBe core.hands.getHand(pId).toList
            case _ => fail("Not valid Hand for " + pId)

      "in Ended" in:
        val serverState = GameState.Ended(players, Scoreboard.empty)
        PlayerGameState.from(serverState, p1) match
          case GameState.Ended(pIds, _) => pIds shouldBe players
          case _                        => fail()
