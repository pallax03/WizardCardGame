package it.unibo.pps.wizard.codecs.engine.model.lobby

import io.circe.parser._
import io.circe.syntax._
import it.unibo.pps.wizard.engine.model.basic.PlayerId
import it.unibo.pps.wizard.engine.model.lobby._
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
      val jsonString = lobby.asJson.noSpaces
      jsonString shouldBe """{"uuid":"uuid-1234","players":[{"id":1,"name":"Alice","bot":null},{"id":2,"name":"Bot-1","bot":"Dumb"}],"status":"WAITING"}"""
      decode[Lobby](jsonString).value shouldBe lobby
