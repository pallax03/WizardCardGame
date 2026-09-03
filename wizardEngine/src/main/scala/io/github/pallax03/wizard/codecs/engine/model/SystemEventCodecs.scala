package io.github.pallax03.wizard.codecs.engine.model

import io.circe._
import io.circe.syntax._
import io.github.pallax03.wizard.engine.model.events.SystemEvent
import io.github.pallax03.wizard.codecs.engine.model.basic.PlayerIdCodecs.given

object SystemEventCodecs:
  given Encoder[SystemEvent] = Encoder.instance: ev =>
    Json.obj(
      "type" -> Json.fromString("system"),
      "playerId" -> ev.playerId.asJson,
      "action" -> Json.fromString(ev.action)
    )
