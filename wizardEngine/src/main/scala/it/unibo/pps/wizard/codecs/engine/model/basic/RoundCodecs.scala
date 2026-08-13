package it.unibo.pps.wizard.codecs.engine.model.basic

import io.circe.*
import it.unibo.pps.wizard.engine.model.basic.*

object RoundCodecs:
  import gameplay.Round
  
  given KeyEncoder[Round] = KeyEncoder.instance(_.toString)
  given KeyDecoder[Round] = KeyDecoder.instance(_.toIntOption)