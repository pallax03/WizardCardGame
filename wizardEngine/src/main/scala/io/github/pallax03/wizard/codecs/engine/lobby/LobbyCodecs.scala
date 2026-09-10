package io.github.pallax03.wizard.codecs.engine.lobby

import scala.util.Try

import io.circe.*
import io.circe.generic.semiauto.*

import io.github.pallax03.wizard.codecs.engine.model.basic.PlayerIdCodecs.given
import io.github.pallax03.wizard.engine.lobby.*

import sttp.tapir.Schema
import sttp.tapir.generic.auto.*

object LobbyCodecs:

  given Encoder[LobbyId] = Encoder.encodeString.contramap(_.toString)
  given Decoder[LobbyId] = Decoder.decodeString.emapTry(s => Try(LobbyId(s)))

  given Encoder[LobbyStatus] = Encoder.encodeString.contramap(_.toString)
  given Decoder[LobbyStatus] =
    Decoder.decodeString.emapTry(s => Try(LobbyStatus.valueOf(s)))

  given Encoder[BotsDifficulty] = Encoder.encodeString.contramap(_.toString)
  given Decoder[BotsDifficulty] =
    Decoder.decodeString.emapTry(s => Try(BotsDifficulty.valueOf(s)))

  given Encoder[Player] = Encoder.forProduct6("id", "name", "difficulty", "isOnline", "strikes", "secret")(p =>
    (p.id, p.name, p.difficulty, p.isOnline, p.strikes, p.secret)
  )
  given Decoder[Player] =
    Decoder.forProduct6("id", "name", "difficulty", "isOnline", "strikes", "secret")(Player.apply)

  given Encoder[GameConfiguration] = deriveEncoder
  given Decoder[GameConfiguration] = deriveDecoder

  given Encoder[Lobby] =
    Encoder.forProduct5("lobbyId", "players", "status", "configuration", "version")(l =>
      (l.uuid, l.players, l.status, l.configuration, l.version)
    )
  given Decoder[Lobby] =
    Decoder.forProduct5("lobbyId", "players", "status", "configuration", "version")(Lobby.apply)

  given Schema[LobbyId] = Schema.string
  given Schema[LobbyStatus] = Schema.string
  given Schema[BotsDifficulty] = Schema.string
  given Schema[Player] = Schema.derived
  given Schema[GameConfiguration] = Schema.derived
  given Schema[Lobby] = Schema.derived

  given Encoder[LobbyError] = Encoder.instance:
    case LobbyError.GameActionRejected(code) => Json.obj("code" -> Json.fromString(code))
    case LobbyError.ConfigurationInvalid(err) =>
      Json.obj(
        "code" -> Json.fromString(err.productPrefix),
        "min" -> Json.fromInt(err.min),
        "max" -> Json.fromInt(err.max)
      )
    case err => Json.obj("code" -> Json.fromString(err.productPrefix))
  given Decoder[LobbyError] = Decoder.instance: _ =>
    Right(null.asInstanceOf[LobbyError])
  given Schema[LobbyError] = Schema.derived[LobbyError]
