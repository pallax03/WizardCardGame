package it.unibo.pps.wizard.codecs.engine.model.core

import io.circe._
import it.unibo.pps.wizard.codecs.engine.model.basic._
import it.unibo.pps.wizard.engine.model.basic._
import it.unibo.pps.wizard.engine.model.core.CoreState

object CoreStateCodecs:
  import PlayerIdCodecs.given
  import HandsCodecs.given
  import TrumpCodecs.given
  import ScoreboardCodecs.given

  given Codec[CoreState] = Codec.AsObject.derived[CoreState]
