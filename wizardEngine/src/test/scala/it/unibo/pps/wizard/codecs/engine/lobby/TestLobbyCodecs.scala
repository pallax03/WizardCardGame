package it.unibo.pps.wizard.codecs.engine.lobby

import it.unibo.pps.wizard.codecs.syntax.CodecSyntax.*
import it.unibo.pps.wizard.engine.lobby._
import it.unibo.pps.wizard.engine.model.basic.PlayerId
import org.scalatest.EitherValues._
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class TestLobbyCodecs extends AnyWordSpec with Matchers:
  
  import LobbyCodecs.given

  "LobbyCodecs" should:
    val players = List(
      LobbyPlayer(PlayerId(1), "Alice", None),
      LobbyPlayer(PlayerId(2), "Bot-1", Some(BotsDifficulty.Dumb))
    )
    val lobby = Lobby(LobbyId("uuid-1234"), players, LobbyStatus.WAITING)
    "encode and decode Lobby correctly" in:
      val jsonString = lobby.toJsonString
      jsonString shouldBe """{"uuid":"uuid-1234","players":[{"id":1,"name":"Alice","bot":null},{"id":2,"name":"Bot-1","bot":"Dumb"}],"status":"WAITING"}"""
      jsonString.decodeAs[Lobby].value shouldBe lobby
