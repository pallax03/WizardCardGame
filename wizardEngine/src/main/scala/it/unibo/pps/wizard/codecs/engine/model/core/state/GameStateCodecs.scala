package it.unibo.pps.wizard.codecs.engine.model.core.state

import io.circe._
import it.unibo.pps.wizard.codecs.engine.model._
import it.unibo.pps.wizard.engine.model.core.state.GameState
import it.unibo.pps.wizard.engine.model.core.state.PlayerCoreState
import it.unibo.pps.wizard.engine.model.core.state.PlayerGameState
import it.unibo.pps.wizard.engine.model.core.state.ServerCoreState
import it.unibo.pps.wizard.engine.model.core.state.ServerGameState

import scala.annotation.nowarn

object GameStateCodecs:
  import CoreStateCodecs.given
  import basic.BiddingCodecs.given
  import basic.PlayerIdCodecs.given
  import basic.ScoreboardCodecs.given
  import basic.TableCodecs.given

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
  given playerPlayingCodec: Codec[GameState.Playing[PlayerCoreState]] = Codec.AsObject.derived
  given playerGameStateCodec: Codec[PlayerGameState] = Codec.AsObject.derived

// IMPORTANT:
// When need to trigger a InconsistentState when Codecs give
// a decoding failure, and gameEngine cannot parse that from redis.
@main @nowarn
def tryGameStateNotWorking(): Unit =
  import GameStateCodecs.given
  import it.unibo.pps.wizard.codecs.syntax.CodecSyntax.*
  val INVALID_GameJson =
    """{"Playing":{"core":{"playersIds":[0,1,2],"hands":{"0":[{"type":"Standard","color":"Yellow","rank":5}],"1":[{"type":"Wizard","id":0}],"2":[{"type":"Standard","color":"Blue","rank":5}]},"trump":{"type":"WizardResolved","card":{"type":"Wizard","id":3},"color":"Green"},"round":1,"dealerId":0,"scoreboard":{}},"bids":{"0":1,"1":0,"2":1},"playerTurn":0}}"""
  // minimum Playing State
  val VALID_GameJson =
    """{"Playing":{"core":{"playersIds":[0,1,2],"hands":{"0":[{"type":"Standard","color":"Yellow","rank":5}],"1":[{"type":"Wizard","id":0}],"2":[{"type":"Standard","color":"Blue","rank":5}]},"trump":{"type":"WizardResolved","card":{"type":"Wizard","id":3},"color":"Green"},"round":1,"dealerId":0,"scoreboard":{}},"bids":{"0":1,"1":0,"2":1},"table":{"playedCards":[]},"playerTurn":0,"tricksWon":{}}}"""
  print(VALID_GameJson.decodeAs[ServerGameState])
