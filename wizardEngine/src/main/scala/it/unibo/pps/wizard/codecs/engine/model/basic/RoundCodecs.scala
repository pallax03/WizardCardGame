package it.unibo.pps.wizard.codecs.engine.model.basic

import io.circe._
import it.unibo.pps.wizard.engine.model.basic._

object RoundCodecs:
  import gameplay.Round

  given KeyEncoder[Round] = KeyEncoder.instance(_.toString)
  given KeyDecoder[Round] = KeyDecoder.instance(_.toIntOption)
