package io.github.pallax03.wizard.codecs.engine.model.basic

import io.circe.*

import io.github.pallax03.wizard.engine.model.basic.*

/**
 * Circe map key codecs for round number indices.
 */
object RoundCodecs:
  import gameplay.Round

  given KeyEncoder[Round] = KeyEncoder.instance(_.toString)
  given KeyDecoder[Round] = KeyDecoder.instance(_.toIntOption)
