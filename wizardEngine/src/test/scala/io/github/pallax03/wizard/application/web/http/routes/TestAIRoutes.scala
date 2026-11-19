package io.github.pallax03.wizard.application.web.http.routes

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

import io.github.pallax03.wizard.*
import io.github.pallax03.wizard.application.web.http.HttpTestDSL

import sttp.client4.Backend
import sttp.model.StatusCode

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class TestAIRoutes extends AnyWordSpec with Matchers with HttpTestDSL:

  import codecs.engine.model.basic.CardCodecs.given
  import codecs.syntax.CodecSyntax.*
  import engine.lobby.*
  import engine.model.basic.*
  import engine.model.basic.bidding.{Bid, Bids, Tricks}
  import engine.model.basic.cards.Card
  import engine.model.basic.gameplay.{Round, Table}
  import engine.model.core.GameException
  import engine.model.core.state.{GameState, PlayerCoreState, ServerCoreState}
  import engine.ports.{AIError, InMemoryLobbyStatePort, InboundPort, StubAIPort, StubInboundPort}

  "AIRoutes" when:
    val p1 = PlayerId(1)
    val secret = "p1-secret"
    val hintPath = s"/api/lobby/$defaultLobbyId/hint"
    val core = PlayerCoreState.from(
      ServerCoreState
        .initialize(List(p1, PlayerId(2), PlayerId(3)), Round.start)
        .copy(
          hands = handsOf(p1 holds ((Five of Blue) - (Ten of Red))),
          trump = Option(Seven of Green).asTrump
        ),
      p1
    )
    val choosingTrump = GameState.ChoosingTrump(core)
    val lobbyPort = InMemoryLobbyStatePort(
      (human(p1, secret) - bot(PlayerId(2)) - bot(PlayerId(3))).asLobby(LobbyStatus.IN_GAME)
    )

    def withAIRoutes(
        inbound: InboundPort,
        trump: Option[Card.Color] = Some(Blue),
        bid: Option[Bid] = Some(1),
        card: Option[Card] = Some(Five of Blue)
    )(test: Backend[Future] ?=> Unit): Unit =
      withEndpoints(AIRoutes(lobbyPort, StubAIPort(inbound, trump, bid, card)).all)(test)

    "serving authenticated hint queries" should:
      "return 200 with AI suggestions for each matching phase" in:
        List(
          ("choose", choosingTrump, """"Blue""""),
          ("bid", GameState.Bidding(core, Bids.empty, p1), "1"),
          (
            "card",
            GameState.Playing(core, Bids.empty, Table.empty, p1, Tricks.empty),
            (Five of Blue).toJson
          )
        ).foreach: (segment, state, expectedJson) =>
          withAIRoutes(StubInboundPort.returning(state)):
            val res = get(s"$hintPath/$segment", bearer = secret)
            res.code shouldBe StatusCode.Ok
            res.body.value shouldBe expectedJson

    "mapping authentication and AI errors" should:
      "return 401 when token is invalid and 404 when game is not found" in:
        withAIRoutes(StubInboundPort.failing(GameException.GameNotFound)):
          get(s"$hintPath/card", bearer = "wrong").code shouldBe StatusCode.Unauthorized

          val notFound = get(s"$hintPath/card", bearer = secret)
          notFound.code shouldBe StatusCode.NotFound
          notFound.errorBody shouldBe errorJson(LobbyError.NotFound(GameException.GameNotFound))

      "return 400 InvalidPhase or NoHintFound on phase mismatch or empty hint" in:
        withAIRoutes(StubInboundPort.returning(choosingTrump)):
          val wrongPhase = get(s"$hintPath/bid", bearer = secret)
          wrongPhase.code shouldBe StatusCode.BadRequest
          wrongPhase.errorBody shouldBe
            errorJson(LobbyError.IAHintError(AIError.InvalidPhase(choosingTrump)))

        withAIRoutes(
          StubInboundPort.returning(
            GameState.Playing(core, Bids.empty, Table.empty, p1, Tricks.empty)
          ),
          card = None
        ):
          val noHint = get(s"$hintPath/card", bearer = secret)
          noHint.code shouldBe StatusCode.BadRequest
          noHint.errorBody shouldBe errorJson(LobbyError.IAHintError(AIError.NoHintFound))
