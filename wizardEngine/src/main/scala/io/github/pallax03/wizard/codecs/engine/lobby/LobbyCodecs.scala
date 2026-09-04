package io.github.pallax03.wizard.codecs.engine.lobby

import scala.util.Try

import io.circe.*

import io.github.pallax03.wizard.codecs.engine.model.basic.PlayerIdCodecs.given
import io.github.pallax03.wizard.engine.lobby.*

import sttp.tapir.Schema

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

  given Encoder[Player] = Encoder.forProduct5("id", "name", "difficulty", "isOnline", "secret")(p =>
    (p.id, p.name, p.difficulty, p.isOnline, p.secret)
  )
  given Decoder[Player] = Decoder.forProduct5("id", "name", "difficulty", "isOnline", "secret")(Player.apply)

  given Encoder[Lobby] =
    Encoder.forProduct3("lobbyId", "players", "status")(l => (l.uuid, l.players, l.status))
  given Decoder[Lobby] = Decoder.forProduct3("lobbyId", "players", "status")(Lobby.apply)

  // --- Tapir Schemas ---

  given Schema[LobbyId] = Schema.string
  given Schema[LobbyStatus] = Schema.string
  given Schema[BotsDifficulty] = Schema.string
  given Schema[Player] = Schema.derived
  given Schema[Lobby] = Schema.derived
