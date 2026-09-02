package io.github.pallax03.wizard.codecs.engine.model.core.state

import io.circe._
import io.circe.syntax._

import io.github.pallax03.wizard.codecs.engine.model._
import io.github.pallax03.wizard.engine.model.core.state._
import io.github.pallax03.wizard.engine.model.rules.TableRules._

object GameStateCodecs:
  import io.circe.generic.auto.given
  import basic.BiddingCodecs.given
  import basic.PlayerIdCodecs.given
  import basic.ScoreboardCodecs.given
  import basic.TableCodecs.given
  import basic.HandsCodecs.given
  import basic.TrumpCodecs.given

  given Codec[GameState.Ended] = Codec.AsObject.derived

  // Server Codecs
  given serverChoosingTrumpCodec: Codec[GameState.ChoosingTrump[ServerCoreState]] =
    Codec.AsObject.derived
  given serverBiddingCodec: Codec[GameState.Bidding[ServerCoreState]] = Codec.AsObject.derived
  given serverPlayingCodec: Codec[GameState.Playing[ServerCoreState]] = Codec.AsObject.derived
  given serverGameStateCodec: Codec[ServerGameState] = Codec.AsObject.derived

  // Player Codecs
  given playerChoosingTrumpCodec: Codec[GameState.ChoosingTrump[PlayerCoreState]] =
    Codec.AsObject.derived
  given playerBiddingCodec: Codec[GameState.Bidding[PlayerCoreState]] = Codec.AsObject.derived

  given playerPlayingCodec: Codec[GameState.Playing[PlayerCoreState]] =
    val derivedCodec = Codec.AsObject.derived[GameState.Playing[PlayerCoreState]]
    Codec.from(
      derivedCodec,
      Encoder.instance { playing =>
        val baseJson = derivedCodec(playing)
        val winner = playing.table.evaluateTrick(playing.core.trump).flatMap(playing.table.playerOf)
        baseJson.mapObject(_.add("currentWinner", winner.asJson))
      }
    )
  given playerGameStateCodec: Codec[PlayerGameState] = Codec.AsObject.derived
