package io.github.pallax03.wizard.codecs.engine.model.basic

import io.circe._
import io.github.pallax03.wizard.engine.model.basic.PlayerId

object PlayerIdCodecs:

  given Encoder[PlayerId] = Encoder.encodeInt.contramap(_.toInt)
  given Decoder[PlayerId] = Decoder.decodeInt.map(PlayerId(_))

  given Codec[PlayerId] = Codec.from(Decoder[PlayerId], Encoder[PlayerId])

  given KeyEncoder[PlayerId] = KeyEncoder.instance(_.toString)
  given KeyDecoder[PlayerId] = KeyDecoder.instance(_.toIntOption.map(_.asInstanceOf[PlayerId]))
