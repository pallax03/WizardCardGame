package io.github.pallax03.wizard.codecs.engine.lobby

import io.github.pallax03.wizard.codecs.syntax.CodecSyntax.*
import io.github.pallax03.wizard.engine.lobby.*
import io.github.pallax03.wizard.engine.model.basic.PlayerId

import org.scalatest.EitherValues.*
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class TestLobbyCodecs extends AnyWordSpec with Matchers:

  import LobbyCodecs.given

  "LobbyCodecs" should:
    val players = List(
      Player(PlayerId(1), "Alice", None),
      Player(PlayerId(2), "Bot-1", Some(BotsDifficulty.Dumb))
    )
    val lobby = Lobby(LobbyId("uuid-1234"), players, LobbyStatus.WAITING)
    "encode and decode Lobby correctly" in:
      val jsonString = lobby.toJson
      jsonString shouldBe """{"lobbyId":"uuid-1234","players":[{"id":1,"name":"Alice","difficulty":null,"isOnline":false},{"id":2,"name":"Bot-1","difficulty":"Dumb","isOnline":false}],"status":"WAITING"}"""
      jsonString.decodeAs[Lobby].value shouldBe lobby
