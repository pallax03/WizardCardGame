package it.unibo.pps.wizard.codecs.engine.model.basic

import io.circe.*
import it.unibo.pps.wizard.engine.model.basic.{PlayerId, PlayerName, Player}

object PlayerCodecs:

  given Encoder[PlayerId] = Encoder.encodeInt.asInstanceOf[Encoder[PlayerId]]
  given Decoder[PlayerId] = Decoder.decodeInt.asInstanceOf[Decoder[PlayerId]]
  given KeyEncoder[PlayerId] = KeyEncoder.instance(_.toString)
  given KeyDecoder[PlayerId] = KeyDecoder.instance(_.toIntOption.map(_.asInstanceOf[PlayerId]))

  given Encoder[PlayerName] = Encoder.encodeString.asInstanceOf[Encoder[PlayerName]]
  given Decoder[PlayerName] = Decoder.decodeString.asInstanceOf[Decoder[PlayerName]]

  given Encoder[Player] = Encoder.forProduct3("id", "name", "isBot")(p => (p.id, p.name, p.isBot))
  given Decoder[Player] = Decoder.forProduct3("id", "name", "isBot")(Player.apply)