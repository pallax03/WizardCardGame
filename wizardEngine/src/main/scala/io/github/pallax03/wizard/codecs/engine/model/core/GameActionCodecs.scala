package io.github.pallax03.wizard.codecs.engine.model.core

import io.circe._
import io.circe.syntax._

import io.github.pallax03.wizard.codecs.combinators.DiscriminatedCodecs._
import io.github.pallax03.wizard.codecs.engine.model.basic.CardCodecs.given
import io.github.pallax03.wizard.codecs.engine.model.basic.PlayerIdCodecs.given
import io.github.pallax03.wizard.engine.model.core.GameAction

object GameActionCodecs:
  given Encoder[GameAction] = Encoder.instance: a =>
    val fields = a match
      case GameAction.ResolveTrumpColor(_, color) => "color" -> color.asJson
      case GameAction.PlaceBid(_, bid)            => "bid" -> bid.asJson
      case GameAction.PlayCard(_, card)           => "card" -> card.asJson

    val baseFields = List(
      "action" -> a.productPrefix.asJson,
      "playerId" -> a.playerId.asJson
    )
    Json.obj(baseFields :+ fields*)

  given Decoder[GameAction] = decodeByTag("action"):
    case "ResolveTrumpColor" =>
      Decoder.forProduct2("playerId", "color")(GameAction.ResolveTrumpColor.apply)
    case "PlaceBid" => Decoder.forProduct2("playerId", "bid")(GameAction.PlaceBid.apply)
    case "PlayCard" => Decoder.forProduct2("playerId", "card")(GameAction.PlayCard.apply)
