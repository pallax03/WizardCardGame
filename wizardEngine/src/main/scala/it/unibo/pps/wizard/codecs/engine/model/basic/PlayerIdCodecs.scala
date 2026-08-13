package it.unibo.pps.wizard.codecs.engine.model.basic

import io.circe._
import it.unibo.pps.wizard.engine.model.basic.PlayerId

object PlayerIdCodecs:

  given Encoder[PlayerId] = Encoder.encodeInt.contramap(_.toInt)
  given Decoder[PlayerId] = Decoder.decodeInt.map(PlayerId(_))

  given Codec[PlayerId] = Codec.from(Decoder[PlayerId], Encoder[PlayerId])

  given KeyEncoder[PlayerId] = KeyEncoder.instance(_.toString)
  given KeyDecoder[PlayerId] = KeyDecoder.instance(_.toIntOption.map(_.asInstanceOf[PlayerId]))
