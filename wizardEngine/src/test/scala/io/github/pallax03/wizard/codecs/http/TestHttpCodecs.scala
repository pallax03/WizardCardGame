package io.github.pallax03.wizard.codecs.http

import io.github.pallax03.wizard.application.web.http.*
import io.github.pallax03.wizard.codecs.syntax.CodecSyntax.*
import io.github.pallax03.wizard.engine.*

import org.scalatest.EitherValues.*
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class TestHttpCodecs extends AnyWordSpec with Matchers:

  import HttpCodecs.given
  import LobbyRequestCodecs.given
  import lobby.{BotsDifficulty, GameConfiguration, LobbyId, LobbyStatus}
  import model.basic.PlayerId

  "HttpCodecs" should:
    "encode and decode AuthLobbyPlayer correctly" in:
      val auth = AuthLobbyPlayer(LobbyId("uuid-1234"), PlayerId(1), Some("secret-xyz"))
      val jsonString = auth.toJson
      jsonString shouldBe """{"lobbyId":"uuid-1234","playerId":1,"secret":"secret-xyz"}"""
      jsonString.decodeAs[AuthLobbyPlayer].value shouldBe auth

  "LobbyRequestCodecs" should:
    "encode and decode JoinLobbyRequest correctly" in:
      val req = JoinLobbyRequest("Alice", None, Some("secret-xyz"))
      val jsonString = req.toJson
      jsonString shouldBe """{"name":"Alice","difficulty":null,"secret":"secret-xyz"}"""
      jsonString.decodeAs[JoinLobbyRequest].value shouldBe req

    "encode and decode PublicPlayerInfo correctly" in:
      val info =
        PublicPlayerInfo(
          PlayerId(2),
          "Bot-1",
          Some(BotsDifficulty.Prolog),
          isOnline = true,
          strikes = 1
        )
      val jsonString = info.toJson
      jsonString shouldBe """{"id":2,"name":"Bot-1","difficulty":"Prolog","isOnline":true,"strikes":1}"""
      jsonString.decodeAs[PublicPlayerInfo].value shouldBe info

    "encode and decode LobbyStateResponse correctly" in:
      val player = PublicPlayerInfo(PlayerId(1), "Alice", None, isOnline = true, strikes = 0)
      val state = LobbyStateResponse(
        LobbyId("uuid-1234"),
        LobbyStatus.WAITING,
        List(player),
        GameConfiguration(),
        123456789L
      )
      val jsonString = state.toJson
      jsonString shouldBe """{"lobbyId":"uuid-1234","status":"WAITING","players":[{"id":1,"name":"Alice","difficulty":null,"isOnline":true,"strikes":0}],"configuration":{"timer":30,"maxStrikes":2},"createdAt":123456789}"""
      jsonString.decodeAs[LobbyStateResponse].value shouldBe state
