package io.github.pallax03.wizard.engine.lobby

import io.github.pallax03.wizard.engine.model.basic.PlayerId
import io.github.pallax03.wizard.engine.model.core.GameException

import org.scalatest.EitherValues
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class TestLobby extends AnyWordSpec with Matchers with EitherValues:

  import LobbyTestDSL.*

  "A Lobby" when:
    val p1 = PlayerId(0)
    val aliceSecret = "alice-token"
    val alice = human(p1, aliceSecret)
    val bob = human(PlayerId(1))
    val ai = bot(PlayerId(2))

    "checking lifecycle states and identifiers" should:
      "report whether an active game exists" in:
        LobbyStatus.WAITING.existGame shouldBe false
        LobbyStatus.FINISHED.existGame shouldBe false
        LobbyStatus.IN_GAME.existGame shouldBe true
        LobbyStatus.DISCONNECTING.existGame shouldBe true
        LobbyStatus.PAUSED.existGame shouldBe true

      "generate unique identifiers" in:
        LobbyId.generate should not be LobbyId.generate

    "authenticating players" should:
      val lobby = (alice - ai).asLobby()

      "succeed with matching secret and reject unknown secrets" in:
        lobby.authenticate(aliceSecret) shouldBe Right(alice)
        lobby.authenticate("wrong") shouldBe Left(LobbyError.NotAuthenticated)

    "validating game start or resume" should:
      "require at least 3 players" in:
        (alice - bob).asLobby().validateStartOrResume shouldBe Left(LobbyError.NotEnoughPlayers)

      "require all active humans to be online" in:
        (alice - bob.offline - ai).asLobby().validateStartOrResume shouldBe Left(
          LobbyError.PlayersOffline
        )

      "allow starting from WAITING or resuming from PAUSED" in:
        val ready = (alice - bob - ai).asLobby()
        ready.validateStartOrResume shouldBe Right(())
        ready.copy(status = LobbyStatus.PAUSED).validateStartOrResume shouldBe Right(())
        ready.copy(status = LobbyStatus.IN_GAME).validateStartOrResume shouldBe Left(
          LobbyError.GameInProgress
        )

    "adding and removing players" should:
      "assign sequential IDs and handle idempotent secret reconnects" in:
        val step1 = emptyLobby.addPlayer("Alice", None, Some(aliceSecret)).value
        step1._1.id shouldBe p1
        step1._2.addPlayer("Alice", None, Some(aliceSecret)).value shouldBe step1

      "reject new players when full or in progress" in:
        val full =
          (0 until GameConfiguration.MAX_PLAYERS).map(i => human(PlayerId(i))).toList.asLobby()
        full.addPlayer("Extra", None, Some("s")) shouldBe Left(LobbyError.Full)
        (alice - bob - ai).asLobby(LobbyStatus.IN_GAME).addPlayer("Late", None, None) shouldBe
          Left(LobbyError.GameInProgress)

      "remove an existing player or return PlayerNotFound" in:
        val lobby = (alice - bob).asLobby()
        lobby.removePlayer(p1).value.players shouldBe List(bob)
        lobby.removePlayer(PlayerId(99)) shouldBe Left(
          LobbyError.NotFound(GameException.PlayerNotFound(PlayerId(99)))
        )

    "handling online status changes" should:
      val active = (alice - bob - ai).asLobby(LobbyStatus.IN_GAME)

      "transition to DISCONNECTING on offline and back to IN_GAME on reconnect" in:
        val disconnecting = active.handleOnlineStatusChange(p1, false).value
        disconnecting.status shouldBe LobbyStatus.DISCONNECTING
        disconnecting.handleOnlineStatusChange(p1, true).value.status shouldBe LobbyStatus.IN_GAME

      "restore human seat and clear strikes when a bot-replaced player reconnects" in:
        val withReplaced =
          (alice.withStrikes(2).replaceWithABot() - bob - ai).asLobby(LobbyStatus.IN_GAME)
        val restored = withReplaced.handleOnlineStatusChange(p1, true).value.players.head
        restored.isHumanPlaying shouldBe true
        restored.strikes shouldBe 0

    "managing strikes and bot failover" should:
      "increment, clamp at zero, and reset player strikes" in:
        val lobby = List(alice.withStrikes(1)).asLobby()
        val inc = lobby.updateStrikes(p1, 1).value
        inc._1 shouldBe 2
        inc._2.updateStrikes(p1, -5).value._1 shouldBe 0
        inc._2.resetStrikes(p1).value.players.head.strikes shouldBe 0

      "replace offline humans with bots and pause if no humans remain" in:
        val oneOffline = (alice.offline - bob - ai).asLobby(LobbyStatus.DISCONNECTING)
        oneOffline.replaceOfflinePlayersWithBots()._2.status shouldBe LobbyStatus.IN_GAME

        val allOffline = (alice.offline - bob.offline - ai).asLobby(LobbyStatus.DISCONNECTING)
        allOffline.replaceOfflinePlayersWithBots()._2.status shouldBe LobbyStatus.PAUSED
