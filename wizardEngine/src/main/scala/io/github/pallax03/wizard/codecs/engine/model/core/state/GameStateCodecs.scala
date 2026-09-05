package io.github.pallax03.wizard.codecs.engine.model.core.state

import io.circe.*
import io.circe.syntax.*

import io.github.pallax03.wizard.codecs.engine.model.*
import io.github.pallax03.wizard.engine.model.core.state.*
import io.github.pallax03.wizard.engine.model.rules.TableRules.*

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
        val winner = playing.table.evaluateTrick(playing.core.trump).map(playing.table.playerOf)
        baseJson.mapObject(_.add("currentWinner", winner.asJson))
      }
    )
  given playerGameStateCodec: Codec[PlayerGameState] = Codec.AsObject.derived

  import sttp.tapir.Schema
  import sttp.tapir.SchemaType
  given Schema[PlayerGameState] = Schema(SchemaType.SProduct(Nil))
