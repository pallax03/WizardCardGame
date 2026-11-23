package io.github.pallax03.wizard.application.web.http.routes

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

import io.github.pallax03.wizard.*
import io.github.pallax03.wizard.application.web.http.HttpTestDSL

import sttp.client4.Backend
import sttp.model.StatusCode

import org.scalatest.OptionValues
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class TestActionRoutes extends AnyWordSpec with Matchers with OptionValues with HttpTestDSL:

  import application.web.ApiSupport
  import codecs.engine.model.basic.CardCodecs.given
  import engine.lobby.*
  import engine.model.basic.*
  import engine.model.basic.cards.Card
  import engine.model.core.{
    AbortedGameException,
    GameAction,
    GameActionError,
    GameException,
    RecoveredGameException
  }
  import engine.ports.{InMemoryLobbyStatePort, StubInboundPort}

  "ActionRoutes" when:
    val p1 = PlayerId(1)
    val secret = "p1-secret"
    val lobbyPath = ApiSupport.lobbyPath(defaultLobbyId)
    val activeLobby = (human(p1, secret).withStrikes(2) - bot(PlayerId(2)) - bot(PlayerId(3)))
      .asLobby(LobbyStatus.IN_GAME)

    def withActionRoutes(
        lobby: Lobby = activeLobby,
        actionResult: Either[GameActionError, Unit] = Right(())
    )(test: Backend[Future] ?=> (InMemoryLobbyStatePort, StubInboundPort) => Unit): Unit =
      val lobbyPort = InMemoryLobbyStatePort(lobby)
      val inbound = StubInboundPort(actionResult = Future.successful(actionResult))
      withEndpoints(ActionRoutes(lobbyPort, inbound).all)(test(lobbyPort, inbound))

    "handling authentication and lobby phase" should:
      "reject unauthenticated requests without invoking InboundPort" in:
        withActionRoutes(): (_, inbound) =>
          val res = postJson(s"$lobbyPath/play", Five of Blue, bearer = "bad")
          res.code shouldBe StatusCode.Unauthorized
          res.errorBody shouldBe errorJson(LobbyError.NotAuthenticated)
          inbound.submittedActions shouldBe empty

      "reject moves with GamePaused when lobby is not IN_GAME" in:
        withActionRoutes(lobby = activeLobby.copy(status = LobbyStatus.PAUSED)): (_, inbound) =>
          val res = postJson(s"$lobbyPath/choose", Red: Card.Color, bearer = secret)
          res.code shouldBe StatusCode.BadRequest
          res.errorBody shouldBe errorJson(LobbyError.GamePaused)
          inbound.submittedActions shouldBe empty

    "submitting valid and rejected game actions" should:
      "map GameActionError rejections to 400 without clearing AFK strikes" in:
        withActionRoutes(actionResult = Left(GameActionError.NotYourTurn(PlayerId(2)))):
          (lobbyPort, inbound) =>
            val res = postJson(s"$lobbyPath/place", 1, bearer = secret)
            res.code shouldBe StatusCode.BadRequest
            res.errorBody shouldBe errorJson(
              LobbyError.GameActionRejected(GameActionError.NotYourTurn(PlayerId(2)))
            )
            inbound.submittedActions shouldBe List((defaultLobbyId, GameAction.PlaceBid(p1, 1)))
            lobbyPort.clearedStrikes shouldBe empty

      "submit trump, bid, and card actions and clear AFK strikes on success" in:
        withActionRoutes(): (lobbyPort, inbound) =>
          postJson(s"$lobbyPath/choose", Green: Card.Color, bearer = secret).code shouldBe
            StatusCode.Ok
          postJson(s"$lobbyPath/place", 2, bearer = secret).code shouldBe StatusCode.Ok
          postJson(s"$lobbyPath/play", Ten of Red, bearer = secret).code shouldBe StatusCode.Ok

          inbound.submittedActions shouldBe List(
            (defaultLobbyId, GameAction.ResolveTrumpColor(p1, Green)),
            (defaultLobbyId, GameAction.PlaceBid(p1, 2)),
            (defaultLobbyId, GameAction.PlayCard(p1, Ten of Red))
          )
          lobbyPort.clearedStrikes should have size 3
          lobbyPort.currentLobby(defaultLobbyId).value.players.head.strikes shouldBe 0

    "handling unhandled server and recovery exceptions" should:
      "map recovery, game, and generic exceptions to structured 500 responses" in:
        List(
          RecoveredGameException(GameException.TableNoWinner) -> "GAME_RECOVERED",
          AbortedGameException(GameException.CorruptedHand(p1)) -> "GAME_ABORTED",
          GameException.TableNoWinner -> "GAME_EXCEPTION",
          new RuntimeException("boom") -> "INTERNAL_ERROR"
        ).foreach: (ex, expectedCode) =>
          val lobbyPort = InMemoryLobbyStatePort(activeLobby)
          val inbound = StubInboundPort(actionResult = Future.failed(ex))
          withEndpoints(ActionRoutes(lobbyPort, inbound).all):
            val res = postJson(s"$lobbyPath/play", Ten of Red, bearer = secret)
            res.code shouldBe StatusCode.InternalServerError
            res.errorBody shouldBe errorJson(LobbyError.InternalServerError(expectedCode))
