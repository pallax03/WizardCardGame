package io.github.pallax03.wizard.engine.adapters.prolog

import io.github.pallax03.wizard.engine.*

import org.scalatest.EitherValues
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatest.wordspec.AnyWordSpec

class TestWizardPrologAdapter extends AnyWordSpec with Matchers with ScalaFutures with EitherValues:

  given PatienceConfig = PatienceConfig(timeout = Span(2, Seconds), interval = Span(15, Millis))

  import lobby.defaultLobbyId
  import model.basic.*
  import model.basic.bidding.{Bids, Tricks}
  import model.basic.cards.Card
  import model.basic.gameplay.{Round, Table}
  import model.core.{AbortedGameException, GameException, RecoveredGameException}
  import model.core.state.{GameState, PlayerCoreState, PlayerGameState, ServerCoreState}
  import model.rules.BiddingRules.*
  import model.rules.TableRules.*
  import ports.{AIError, StubInboundPort}

  "WizardPrologAdapter" when:
    val p1 = PlayerId(1)
    val p2 = PlayerId(2)
    val p3 = PlayerId(3)
    val players = List(p1, p2, p3)
    val sampleHand = ((Two of Blue) - (Ten of Red) - wizard).asHand
    val baseCore = PlayerCoreState.from(
      ServerCoreState
        .initialize(players, Round.start)
        .copy(hands = handsOf(p1 holds sampleHand.toList), trump = Option(Five of Green).asTrump),
      p1
    )

    def adapterFor(state: PlayerGameState) =
      WizardPrologAdapter(StubInboundPort.returning(state))

    def failingAdapter(error: Throwable) =
      WizardPrologAdapter(StubInboundPort.failing(error))

    "in ChoosingTrump phase" should:
      val adapter = adapterFor(GameState.ChoosingTrump(baseCore))

      "suggest a valid trump color" in:
        Card.Color.values should contain(
          adapter.resolvedTrumpColor(defaultLobbyId, p1).futureValue.value.get
        )

      "reject bid and card queries as InvalidPhase" in:
        adapter.placeBid(defaultLobbyId, p1).futureValue.left.value shouldBe a[AIError.InvalidPhase]
        adapter.bestCard(defaultLobbyId, p1).futureValue.left.value shouldBe a[AIError.InvalidPhase]

    "in Bidding phase" should:
      "adjust bid automatically when the initial Prolog estimate violates the Hook Rule" in:
        val priorBids = Bids.empty + (p1 place 0) + (p2 place 1)
        val bid = adapterFor(
          GameState.Bidding(
            baseCore.copy(hand = (Two of Blue).asHand, trump = Option(jester).asTrump),
            priorBids,
            p3
          )
        ).placeBid(defaultLobbyId, p3).futureValue.value.get
        bid.validateBid(Round.start, priorBids, players.size) shouldBe Right(())

    "in Playing phase" should:
      "recommend only cards legal for the current table" in:
        val table = Table.empty + (p1 plays (Four of Blue))
        sampleHand.legalCards(table) should contain(
          adapterFor(
            GameState.Playing(baseCore, Bids.empty + (p2 place 1), table, p2, Tricks.empty)
          ).bestCard(defaultLobbyId, p2).futureValue.value.get
        )

    "handling engine failures" should:
      "map entity lookup exceptions to AIError.GameException" in:
        List(
          (GameException.GameNotFound, GameException.GameNotFound),
          (AbortedGameException(GameException.GameNotFound), GameException.GameNotFound),
          (
            RecoveredGameException(GameException.PlayerNotFound(p1)),
            GameException.PlayerNotFound(p1)
          )
        ).foreach: (thrown, expected) =>
          failingAdapter(thrown).bestCard(defaultLobbyId, p1).futureValue shouldBe
            Left(AIError.GameException(expected))

      "map corrupted engine state errors to AIError.NoHintFound" in:
        failingAdapter(GameException.CorruptedState("bad"))
          .bestCard(defaultLobbyId, p1)
          .futureValue shouldBe Left(AIError.NoHintFound)
