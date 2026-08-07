package it.unibo.pps.wizard.codecs.engine.model.basic

import io.circe._
import it.unibo.pps.wizard.engine.model.basic.PlayerId

object PlayerIdCodecs:

  given Encoder[PlayerId] = Encoder.encodeInt.asInstanceOf[Encoder[PlayerId]]
  given Decoder[PlayerId] = Decoder.decodeInt.asInstanceOf[Decoder[PlayerId]]
  given KeyEncoder[PlayerId] = KeyEncoder.instance(_.toString)
  given KeyDecoder[PlayerId] = KeyDecoder.instance(_.toIntOption.map(_.asInstanceOf[PlayerId]))
