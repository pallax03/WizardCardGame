package io.github.pallax03.wizard.codecs.engine.lobby

import io.circe.{Codec, Decoder, Encoder}

import io.github.pallax03.wizard.codecs.engine.lobby.LobbyCodecs.given
import io.github.pallax03.wizard.codecs.engine.model.WizardEventsCodecs.given
import io.github.pallax03.wizard.engine.lobby.BotTask

object BotTaskCodecs:
  // WizardEventsCodecs provides separate Encoder + Decoder for WizardEvent.
  // Codec.from combines them so deriveCodec can pick them up for BotTask.
  given Codec[BotTask] = Codec.from(
    Decoder.derived[BotTask],
    Encoder.AsObject.derived[BotTask]
  )
