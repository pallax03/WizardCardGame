package io.github.pallax03.wizard.codecs.http

import io.circe.generic.semiauto.*
import io.circe.{Decoder, Encoder}

import io.github.pallax03.wizard.application.web.http.AuthLobbyPlayer
import io.github.pallax03.wizard.codecs.engine.lobby.LobbyCodecs.given
import io.github.pallax03.wizard.codecs.engine.model.basic.PlayerIdCodecs.given

import sttp.tapir.Schema
import sttp.tapir.generic.auto.*

object HttpCodecs:

  given Encoder[AuthLobbyPlayer] = deriveEncoder
  given Decoder[AuthLobbyPlayer] = deriveDecoder

  given Schema[AuthLobbyPlayer] = Schema.derived
