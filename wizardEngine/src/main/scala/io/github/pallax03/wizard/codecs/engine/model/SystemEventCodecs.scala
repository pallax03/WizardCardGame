package io.github.pallax03.wizard.codecs.engine.model

import io.circe.*
import io.circe.syntax.*

import io.github.pallax03.wizard.codecs.engine.model.basic.PlayerIdCodecs.given
import io.github.pallax03.wizard.engine.model.events.SystemEvent

object SystemEventCodecs:
  given Encoder[SystemEvent] = Encoder.instance: ev =>
    Json.obj(
      "type" -> Json.fromString("system"),
      "playerId" -> ev.playerId.asJson,
      "action" -> Json.fromString(ev.action)
    )
