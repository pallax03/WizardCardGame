package io.github.pallax03.wizard.codecs.engine.model.basic

import io.circe.*
import sttp.tapir.Schema

import io.github.pallax03.wizard.engine.model.basic.PlayerId

object PlayerIdCodecs:
  // --- Circe ---

  given Encoder[PlayerId] = Encoder.encodeInt.contramap(_.toInt)
  given Decoder[PlayerId] = Decoder.decodeInt.map(PlayerId(_))

  given Codec[PlayerId] = Codec.from(Decoder[PlayerId], Encoder[PlayerId])

  given KeyEncoder[PlayerId] = KeyEncoder.instance(_.toString)
  given KeyDecoder[PlayerId] = KeyDecoder.instance(_.toIntOption.map(_.asInstanceOf[PlayerId]))

  // --- Tapir Schemas ---

  given Schema[PlayerId] = Schema.schemaForInt.map(v => Some(PlayerId(v)))(_.toInt)
