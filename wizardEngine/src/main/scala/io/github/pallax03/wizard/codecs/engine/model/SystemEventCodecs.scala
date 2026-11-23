package io.github.pallax03.wizard.codecs.engine.model

import io.circe.*
import io.circe.syntax.*

import io.github.pallax03.wizard.codecs.engine.model.basic.PlayerIdCodecs.given
import io.github.pallax03.wizard.engine.model.basic.PlayerId
import io.github.pallax03.wizard.engine.model.events.SystemEvent

import sttp.tapir.Schema
import sttp.tapir.generic.auto.*

/** Circe codecs and Tapir schema for [[SystemEvent]] notifications (lobby lifecycle, pauses, player departures). */
object SystemEventCodecs:
  given Encoder[SystemEvent] = Encoder.instance: ev =>
    Json.obj(
      "type" -> Json.fromString("system"),
      "playerId" -> ev.playerId.asJson,
      "action" -> Json.fromString(ev.action)
    )

  given Decoder[SystemEvent] = Decoder.instance: c =>
    for
      _ <- c
        .downField("type")
        .as[String]
        .filterOrElse(_ == "system", DecodingFailure("Expected type 'system'", c.history))
      playerId <- c.downField("playerId").as[PlayerId]
      action <- c.downField("action").as[String]
    yield SystemEvent(playerId, action)

  given Schema[SystemEvent] = Schema.derived
