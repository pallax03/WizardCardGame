package io.github.pallax03.wizard.codecs.engine.model

import io.circe.*
import io.circe.syntax.*

import io.github.pallax03.wizard.codecs.engine.model.basic.PlayerIdCodecs.given
import io.github.pallax03.wizard.engine.model.events.SystemEvent

/** Circe encoder for [[SystemEvent]] notifications (lobby lifecycle, pauses, player departures). */
object SystemEventCodecs:
  given Encoder[SystemEvent] = Encoder.instance: ev =>
    Json.obj(
      "type" -> Json.fromString("system"),
      "playerId" -> ev.playerId.asJson,
      "action" -> Json.fromString(ev.action)
    )
