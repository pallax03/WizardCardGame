package io.github.pallax03.wizard.application.bot.strategy

import scala.concurrent.ExecutionContext.Implicits.global

import io.github.pallax03.wizard.engine.*

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatest.wordspec.AnyWordSpec

class TestBotStrategy extends AnyWordSpec with Matchers with ScalaFutures:

  given PatienceConfig = PatienceConfig(timeout = Span(2, Seconds), interval = Span(15, Millis))

  import lobby.{BotsDifficulty, defaultLobbyId}
  import model.basic.*
  import model.basic.bidding.{Bid, Bids, Tricks}
  import model.basic.cards.Card
  import model.basic.gameplay.{Round, Table}
  import model.core.GameAction
  import model.core.state.{GameState, PlayerCoreState, ServerCoreState}
  import model.events.InvitationEvent.*
  import ports.{StubAIPort, StubInboundPort}

  "BotStrategy" when:
    val botId = PlayerId(1)
    val players = List(botId, PlayerId(2), PlayerId(3))
    val legalCards = (Ten of Blue) - (Five of Red)
    val core = PlayerCoreState.from(
      ServerCoreState
        .initialize(players, Round.start)
        .copy(hands = handsOf(botId holds legalCards), trump = Option(Five of Green).asTrump),
      botId
    )
    val trumpInv = WaitingForTrump(botId)
    val bidInv = WaitingForBid(botId, Round.start, Some(0))
    val cardInv = WaitingForCard(botId, legalCards, isTableEmpty = true)

    def prologStrategy(
        inbound: StubInboundPort,
        trump: Option[Card.Color] = None,
        bid: Option[Bid] = None,
        card: Option[Card] = None
    ): BotStrategy =
      BotStrategy(BotsDifficulty.Prolog, StubAIPort(inbound, trump, bid, card))

    "using Dumb difficulty" should:
      val dumb = BotStrategy(BotsDifficulty.Dumb, StubAIPort(StubInboundPort()))

      "resolve trump, bid, and card moves with legal fallback actions" in:
        dumb.resolveInvitation(defaultLobbyId, trumpInv).futureValue match
          case GameAction.ResolveTrumpColor(p, color) =>
            p shouldBe botId
            Card.Color.values should contain(color)
          case other => fail(s"Unexpected action: $other")

        dumb.resolveInvitation(defaultLobbyId, bidInv).futureValue match
          case GameAction.PlaceBid(p, bid) =>
            p shouldBe botId
            bid should not be 0
          case other => fail(s"Unexpected action: $other")

        dumb.resolveInvitation(defaultLobbyId, cardInv).futureValue shouldBe
          GameAction.PlayCard(botId, Ten of Blue)

    "using Prolog difficulty" should:
      "return AI-suggested actions when hints are available" in:
        List(
          (
            prologStrategy(
              StubInboundPort.returning(GameState.ChoosingTrump(core)),
              trump = Some(Yellow)
            ),
            trumpInv,
            GameAction.ResolveTrumpColor(botId, Yellow)
          ),
          (
            prologStrategy(
              StubInboundPort.returning(GameState.Bidding(core, Bids.empty, botId)),
              bid = Some(1)
            ),
            bidInv,
            GameAction.PlaceBid(botId, 1)
          ),
          (
            prologStrategy(
              StubInboundPort.returning(
                GameState.Playing(core, Bids.empty, Table.empty, botId, Tricks.empty)
              ),
              card = Some(Five of Red)
            ),
            cardInv,
            GameAction.PlayCard(botId, Five of Red)
          )
        ).foreach: (strategy, invitation, expectedAction) =>
          strategy.resolveInvitation(defaultLobbyId, invitation).futureValue shouldBe expectedAction

      "fall back cleanly when AI returns no hint, an AIError, or a failed future" in:
        List(
          StubInboundPort.returning(
            GameState.Playing(core, Bids.empty, Table.empty, botId, Tricks.empty)
          ),
          StubInboundPort(),
          StubInboundPort.failing(new RuntimeException("boom"))
        ).foreach: inbound =>
          prologStrategy(inbound).resolveInvitation(defaultLobbyId, cardInv).futureValue shouldBe
            GameAction.PlayCard(botId, Ten of Blue)
