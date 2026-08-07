package it.unibo.pps.wizard.codecs.engine.model.lobby

import io.circe._
import it.unibo.pps.wizard.engine.model.lobby._

import it.unibo.pps.wizard.codecs.engine.model.basic.PlayerIdCodecs.given

object LobbyCodecs:

  given Encoder[LobbyStatus] = Encoder.encodeString.contramap(_.toString)
  given Decoder[LobbyStatus] =
    Decoder.decodeString.emapTry(s => scala.util.Try(LobbyStatus.valueOf(s)))

  given Encoder[BotsDifficulty] = Encoder.encodeString.contramap(_.toString)
  given Decoder[BotsDifficulty] =
    Decoder.decodeString.emapTry(s => scala.util.Try(BotsDifficulty.valueOf(s)))

  given Encoder[LobbyPlayer] = Encoder.forProduct3("id", "name", "bot")(p => (p.id, p.name, p.bot))
  given Decoder[LobbyPlayer] = Decoder.forProduct3("id", "name", "bot")(LobbyPlayer.apply)

  given Encoder[Lobby] =
    Encoder.forProduct3("uuid", "players", "status")(l => (l.uuid, l.players, l.status))
  given Decoder[Lobby] = Decoder.forProduct3("uuid", "players", "status")(Lobby.apply)
