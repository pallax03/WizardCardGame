package io.github.pallax03.wizard.codecs.engine.lobby

import io.circe.{Codec, Decoder, Encoder}

import io.github.pallax03.wizard.codecs.engine.lobby.LobbyCodecs.given
import io.github.pallax03.wizard.codecs.engine.model.WizardEventsCodecs.given
import io.github.pallax03.wizard.engine.lobby.BotTask

/** Circe codec for [[BotTask]] payload messages exchanged over Redis Streams. */
object BotTaskCodecs:
  given Codec[BotTask] = Codec.from(
    Decoder.derived[BotTask],
    Encoder.AsObject.derived[BotTask]
  )
