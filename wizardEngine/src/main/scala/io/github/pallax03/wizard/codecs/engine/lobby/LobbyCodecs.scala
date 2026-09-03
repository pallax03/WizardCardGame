package io.github.pallax03.wizard.codecs.engine.lobby

import scala.util.Try

import io.circe.*
import sttp.tapir.Schema

import io.github.pallax03.wizard.codecs.engine.model.basic.PlayerIdCodecs.given
import io.github.pallax03.wizard.engine.lobby.*

object LobbyCodecs:
  // --- Circe ---

  given Encoder[LobbyId] = Encoder.encodeString.contramap(_.toString)
  given Decoder[LobbyId] = Decoder.decodeString.emapTry(s => Try(LobbyId(s)))

  given Encoder[LobbyStatus] = Encoder.encodeString.contramap(_.toString)
  given Decoder[LobbyStatus] =
    Decoder.decodeString.emapTry(s => Try(LobbyStatus.valueOf(s)))

  given Encoder[BotsDifficulty] = Encoder.encodeString.contramap(_.toString)
  given Decoder[BotsDifficulty] =
    Decoder.decodeString.emapTry(s => Try(BotsDifficulty.valueOf(s)))

  given Encoder[Player] =
    Encoder.forProduct4("id", "name", "difficulty", "isOnline")(p =>
      (p.id, p.name, p.difficulty, p.isOnline)
    )
  given Decoder[Player] = Decoder.forProduct4("id", "name", "difficulty", "isOnline")(Player.apply)

  given Encoder[Lobby] =
    Encoder.forProduct3("lobbyId", "players", "status")(l => (l.uuid, l.players, l.status))
  given Decoder[Lobby] = Decoder.forProduct3("lobbyId", "players", "status")(Lobby.apply)

  // --- Tapir Schemas ---

  given Schema[LobbyId]       = Schema.string
  given Schema[LobbyStatus]   = Schema.string
  given Schema[BotsDifficulty] = Schema.string
  given Schema[Player]        = Schema.derived
  given Schema[Lobby]         = Schema.derived
