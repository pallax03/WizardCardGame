package it.unibo.pps.wizard.codecs.engine.model.core

import io.circe.*
import io.circe.syntax.*

import it.unibo.pps.wizard.codecs.engine.model.basic.CardCodecs.given

import it.unibo.pps.wizard.codecs.engine.model.basic.PlayerCodecs.given

import it.unibo.pps.wizard.codecs.combinators.DiscriminatedCodecs.*
import it.unibo.pps.wizard.engine.model.core.GameAction

object GameActionCodecs:
  given Encoder[GameAction] = Encoder.instance: a =>
    val fields = a match
      case GameAction.ResolveTrumpColor(_, color) => "color" -> color.asJson
      case GameAction.PlaceBid(_, bid)            => "bid"   -> bid.asJson
      case GameAction.PlayCard(_, card)           => "card"  -> card.asJson
    
    val baseFields = List(
      "action"   -> a.productPrefix.asJson,
      "playerId" -> a.playerId.asJson
    )
    Json.obj(baseFields :+ fields *)
    
  given Decoder[GameAction] = decodeByTag("action"):
    case "ResolveTrumpColor" => Decoder.forProduct2("playerId", "color")(GameAction.ResolveTrumpColor.apply) 
    case "PlaceBid"          => Decoder.forProduct2("playerId", "bid")(GameAction.PlaceBid.apply)
    case "PlayCard"          => Decoder.forProduct2("playerId", "card")(GameAction.PlayCard.apply)