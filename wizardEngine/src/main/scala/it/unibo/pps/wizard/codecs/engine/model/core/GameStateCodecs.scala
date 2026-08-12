package it.unibo.pps.wizard.codecs.engine.model.core

import io.circe.*
import it.unibo.pps.wizard.engine.model.core.GameState
import it.unibo.pps.wizard.codecs.engine.model._

object GameStateCodecs:
  import CoreStateCodecs.given
  import basic.BiddingCodecs.given
  import basic.PlayerIdCodecs.given
  import basic.TableCodecs.given
  import basic.ScoreboardCodecs.given

  given Codec[GameState.ChoosingTrump] = Codec.AsObject.derived[GameState.ChoosingTrump]
  given Codec[GameState.Bidding] = Codec.AsObject.derived[GameState.Bidding]
  given Codec[GameState.Playing] = Codec.AsObject.derived[GameState.Playing]
  given Codec[GameState.Ended] = Codec.AsObject.derived[GameState.Ended]
  given Codec[GameState] = Codec.AsObject.derived[GameState]
