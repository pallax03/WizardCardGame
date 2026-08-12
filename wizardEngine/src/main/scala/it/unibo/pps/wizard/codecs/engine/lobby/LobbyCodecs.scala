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
    Decoder.decodeString.emapTry(s => scala.util.Try(LobbyStatus.valueOf(s)))

  given Encoder[BotsDifficulty] = Encoder.encodeString.contramap(_.toString)
  given Decoder[BotsDifficulty] =
    Decoder.decodeString.emapTry(s => scala.util.Try(BotsDifficulty.valueOf(s)))

  given Encoder[Player] = Encoder.forProduct3("id", "name", "bot")(p => (p.id, p.name, p.bot))
  given Decoder[Player] = Decoder.forProduct3("id", "name", "bot")(Player.apply)

  given Encoder[Lobby] = Encoder.forProduct3("lobbyId", "players", "status")(l => (l.uuid, l.players, l.status))
  given Decoder[Lobby] = Decoder.forProduct3("lobbyId", "players", "status")(Lobby.apply)
