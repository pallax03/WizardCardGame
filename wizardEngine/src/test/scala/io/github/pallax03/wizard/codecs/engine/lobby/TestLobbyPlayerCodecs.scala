package io.github.pallax03.wizard.codecs.engine.lobby

import io.github.pallax03.wizard.codecs.syntax.CodecSyntax.*
import io.github.pallax03.wizard.engine.*

import org.scalatest.EitherValues.*
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class TestLobbyPlayerCodecs extends AnyWordSpec with Matchers:

  import LobbyPlayerCodecs.given
  import lobby.{LobbyId, LobbyPlayer}
  import model.basic.PlayerId

  "LobbyPlayerCodecs" should:
    "encode and decode LobbyPlayer correctly" in:
      val lobbyPlayer = LobbyPlayer(LobbyId("uuid-1234"), PlayerId(1))
      val jsonString = lobbyPlayer.toJson
      jsonString shouldBe """{"lobbyId":"uuid-1234","playerId":1}"""
      jsonString.decodeAs[LobbyPlayer].value shouldBe lobbyPlayer
