package it.unibo.pps.wizard.codecs.engine.model.core.state

import io.circe._
import it.unibo.pps.wizard.codecs.engine.model.basic._
import it.unibo.pps.wizard.engine.model.basic._
import it.unibo.pps.wizard.engine.model.core.state._

object CoreStateCodecs:
  import HandsCodecs.given
  import PlayerIdCodecs.given
  import ScoreboardCodecs.given
  import TrumpCodecs.given

  given Codec[ServerCoreState] = Codec.AsObject.derived[ServerCoreState]
  given Codec[PlayerCoreState] = Codec.AsObject.derived[PlayerCoreState]
