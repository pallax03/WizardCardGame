package it.unibo.pps.wizard.codecs.engine.lobby

import io.circe._
import it.unibo.pps.wizard.codecs.engine.model.basic.PlayerIdCodecs.given
import it.unibo.pps.wizard.engine.lobby._

import scala.util.Try

object LobbyCodecs:

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
