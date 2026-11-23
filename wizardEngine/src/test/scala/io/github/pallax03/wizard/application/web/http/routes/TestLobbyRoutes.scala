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

class TestLobbyRoutes extends AnyWordSpec with Matchers with OptionValues with HttpTestDSL:

  import application.web.http.*
  import codecs.engine.lobby.LobbyCodecs.given
  import codecs.engine.model.basic.PlayerIdCodecs.given
  import codecs.http.HttpCodecs.given
  import codecs.http.LobbyRequestCodecs.given
  import engine.lobby.*
  import engine.model.basic.*
  import engine.model.basic.bidding.Bids
  import engine.model.basic.gameplay.Round
  import engine.model.core.state.{GameState, PlayerGameState, ServerCoreState}
  import engine.ports.{InMemoryLobbyStatePort, StubInboundPort}
  import util.ApiSupport

  "LobbyRoutes" when:
    val aliceId = PlayerId(0)
    val bobId = PlayerId(1)
    val aiId = PlayerId(2)
    val aliceSecret = "alice-secret"
    val alice = human(aliceId, aliceSecret)
    val bob = human(bobId, "bob-secret")
    val ai = bot(aiId)
    val lobbyPath = ApiSupport.lobbyPath(defaultLobbyId)

    def readyLobby(status: LobbyStatus = LobbyStatus.WAITING): Lobby =
      (alice - bob - ai).asLobby(status)

    def withLobbyRoutes(
        lobbies: List[Lobby] = Nil,
        inbound: StubInboundPort = StubInboundPort()
    )(test: Backend[Future] ?=> (InMemoryLobbyStatePort, StubInboundPort) => Unit): Unit =
      val port = InMemoryLobbyStatePort(lobbies*)
      withEndpoints(LobbyRoutes(port, inbound).all)(test(port, inbound))

    "creating and joining a lobby" should:
      "assign a secret to humans and keep bot secret empty" in:
        withLobbyRoutes(): (lobbyPort, _) =>
          val created =
            postJson(ApiSupport.LobbiesBasePath, JoinLobbyRequest("Alice", None))
              .bodyAs[AuthLobbyPlayer]
          created.playerId shouldBe aliceId
          created.secret shouldBe defined

          postJson(
            ApiSupport.lobbyPath(created.lobbyId),
            JoinLobbyRequest("Bot", Some(BotsDifficulty.Dumb), Some("ignored"))
          ).bodyAs[AuthLobbyPlayer].secret shouldBe None
          lobbyPort.currentLobby(created.lobbyId).value.players should have size 2

      "return sanitized lobby info or 404 when missing" in:
        withLobbyRoutes(List((alice - bob).asLobby())): (_, _) =>
          get(lobbyPath).bodyAs[LobbyStateResponse].players.map(_.name) shouldBe
            List("Player-0", "Player-1")
          get(ApiSupport.lobbyPath("unknown")).code shouldBe StatusCode.NotFound

    "starting, pausing, and returning from a game" should:
      "reject unauthenticated start and distinguish fresh start vs paused resume" in:
        withLobbyRoutes(List(readyLobby())): (lobbyPort, inbound) =>
          post(s"$lobbyPath/start", bearer = "wrong-token").code shouldBe StatusCode.Unauthorized
          post(s"$lobbyPath/start", bearer = aliceSecret).code shouldBe StatusCode.Ok
          inbound.startedGames should have size 1
          lobbyPort.currentLobby(defaultLobbyId).value.status shouldBe LobbyStatus.IN_GAME

          post(s"$lobbyPath/pause", bearer = aliceSecret).code shouldBe StatusCode.Ok
          lobbyPort.currentLobby(defaultLobbyId).value.status shouldBe LobbyStatus.PAUSED

          post(s"$lobbyPath/start", bearer = aliceSecret).code shouldBe StatusCode.Ok
          inbound.resumedGames shouldBe List(defaultLobbyId)

          post(s"$lobbyPath/return", bearer = aliceSecret).code shouldBe StatusCode.BadRequest
          post(s"$lobbyPath/pause", bearer = aliceSecret).code shouldBe StatusCode.Ok
          post(s"$lobbyPath/return", bearer = aliceSecret).code shouldBe StatusCode.Ok
          inbound.deletedGames shouldBe List(defaultLobbyId)
          lobbyPort.currentLobby(defaultLobbyId).value.status shouldBe LobbyStatus.WAITING

    "updating configuration and removing players" should:
      "validate configuration bounds and persist valid configs" in:
        withLobbyRoutes(List((alice - bob).asLobby())): (lobbyPort, _) =>
          postJson(
            s"$lobbyPath/configuration",
            GameConfiguration(timer = 10),
            bearer = aliceSecret
          ).code shouldBe StatusCode.BadRequest

          postJson(
            s"$lobbyPath/configuration",
            GameConfiguration(timer = 45, maxStrikes = 3),
            bearer = aliceSecret
          ).code shouldBe StatusCode.Ok
          lobbyPort.currentLobby(defaultLobbyId).value.configuration shouldBe GameConfiguration(
            45,
            3
          )

      "delete empty waiting lobbies and block removal once in game" in:
        withLobbyRoutes(List((alice - ai).asLobby())): (lobbyPort, _) =>
          deleteJson(lobbyPath, aliceId, bearer = aliceSecret).code shouldBe StatusCode.NoContent
          lobbyPort.currentLobby(defaultLobbyId) shouldBe None

        withLobbyRoutes(List(readyLobby(LobbyStatus.IN_GAME))): (_, _) =>
          deleteJson(lobbyPath, aliceId, bearer = aliceSecret).code shouldBe StatusCode.BadRequest

    "fetching player game state" should:
      "reconcile missing engine state back to WAITING and return 404" in:
        withLobbyRoutes(List(readyLobby(LobbyStatus.IN_GAME))): (lobbyPort, _) =>
          get(s"$lobbyPath/game", bearer = aliceSecret).code shouldBe StatusCode.NotFound
          lobbyPort.currentLobby(defaultLobbyId).value.status shouldBe LobbyStatus.WAITING

      "return PlayerGameState when game exists" in:
        withLobbyRoutes(
          List(readyLobby(LobbyStatus.IN_GAME)),
          StubInboundPort.returning(
            PlayerGameState.from(
              GameState.Bidding(
                ServerCoreState
                  .initialize(List(aliceId, bobId, aiId), Round.start)
                  .copy(hands = handsOf(aliceId holds (Two of Blue))),
                Bids.empty,
                aliceId
              ),
              aliceId
            )
          )
        ): (_, _) =>
          get(s"$lobbyPath/game", bearer = aliceSecret).code shouldBe StatusCode.Ok
