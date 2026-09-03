package io.github.pallax03.wizard.codecs.http

import io.circe.generic.semiauto.*
import io.circe.{Decoder, Encoder}
import sttp.tapir.Schema
import sttp.tapir.generic.auto.*

import io.github.pallax03.wizard.application.web.http.{ActionSuccessResponse, ErrorResponse, LobbyPlayer}
import io.github.pallax03.wizard.codecs.engine.lobby.LobbyCodecs.given
import io.github.pallax03.wizard.codecs.engine.model.basic.PlayerIdCodecs.given

/**
 * Circe codecs and Tapir schemas for the shared HTTP response/error types.
 *
 * These types are defined in the application web layer but their serialization
 * contract lives here, following the same codec-per-type pattern used for
 * engine types in [[io.github.pallax03.wizard.codecs.engine]].
 */
object HttpCodecs:
  // --- Circe ---

  given Encoder[ErrorResponse]         = deriveEncoder
  given Decoder[ErrorResponse]         = deriveDecoder
  given Encoder[ActionSuccessResponse] = deriveEncoder
  given Decoder[ActionSuccessResponse] = deriveDecoder
  given Encoder[LobbyPlayer]           = deriveEncoder
  given Decoder[LobbyPlayer]           = deriveDecoder

  // --- Tapir Schemas ---

  given Schema[ErrorResponse]         = Schema.derived
  given Schema[ActionSuccessResponse] = Schema.derived
  given Schema[LobbyPlayer]           = Schema.derived
