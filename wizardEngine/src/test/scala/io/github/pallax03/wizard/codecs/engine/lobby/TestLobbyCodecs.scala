package io.github.pallax03.wizard.codecs.engine.lobby

import io.github.pallax03.wizard.codecs.syntax.CodecSyntax.*
import io.github.pallax03.wizard.engine.*

import org.scalatest.EitherValues.*
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class TestLobbyCodecs extends AnyWordSpec with Matchers:

  import LobbyCodecs.given
  import lobby.*
  import model.basic.PlayerId
  import model.core.GameActionError
  import ports.AIError

  "LobbyCodecs" should:
    val players = List(
      Player(PlayerId(1), "Alice", None),
      Player(PlayerId(2), "Bot-1", Some(BotsDifficulty.Dumb))
    )
    val lobby =
      Lobby(LobbyId("uuid-1234"), players, LobbyStatus.WAITING, GameConfiguration(), 0, 123456789L)

    "encode and decode Lobby correctly" in:
      val jsonString = lobby.toJson
      jsonString shouldBe """{"lobbyId":"uuid-1234","players":[{"id":1,"name":"Alice","difficulty":null,"isOnline":false,"strikes":0,"secret":null},{"id":2,"name":"Bot-1","difficulty":"Dumb","isOnline":false,"strikes":0,"secret":null}],"status":"WAITING","configuration":{"timer":30,"maxStrikes":2},"version":0,"createdAt":123456789}"""
      jsonString.decodeAs[Lobby].value shouldBe lobby

    "default createdAt to 0 when missing in Lobby JSON" in:
      val jsonWithoutCreatedAt =
        """{"lobbyId":"uuid-1234","players":[],"status":"WAITING","configuration":{"timer":30,"maxStrikes":2},"version":1}"""
      jsonWithoutCreatedAt.decodeAs[Lobby].value.createdAt shouldBe 0L

    "encode simple LobbyError variants with their productPrefix" in:
      (LobbyError.Full: LobbyError).toJson shouldBe """{"code":"Full"}"""
      (LobbyError.NotAuthenticated: LobbyError).toJson shouldBe """{"code":"NotAuthenticated"}"""

    "encode wrapped LobbyError variants correctly" in:
      val p1 = PlayerId(1)
      val actionErr: LobbyError = LobbyError.GameActionRejected(GameActionError.NotYourTurn(p1))
      val aiErr: LobbyError = LobbyError.IAHintError(AIError.NoHintFound)
      val notFoundErr: LobbyError = LobbyError.PlayerNotFound(p1)
      val internalErr: LobbyError = LobbyError.InternalServerError("REDIS_DOWN")
      actionErr.toJson shouldBe """{"code":"NotYourTurn"}"""
      aiErr.toJson shouldBe """{"code":"NoHintFound"}"""
      notFoundErr.toJson shouldBe """{"code":"PlayerNotFound"}"""
      internalErr.toJson shouldBe """{"code":"REDIS_DOWN"}"""

    "encode ConfigurationInvalid with min and max bounds" in:
      val configErr: LobbyError =
        LobbyError.ConfigurationInvalid(ConfigurationErrors.TimerNotValid)
      configErr.toJson shouldBe """{"code":"TimerNotValid","min":15,"max":90}"""
