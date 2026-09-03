package io.github.pallax03.wizard.codecs.http

import io.circe.generic.semiauto.*
import io.circe.{Decoder, Encoder}
import sttp.tapir.Schema
import sttp.tapir.generic.auto.*

import io.github.pallax03.wizard.application.web.http.endpoints.{
  GameStartedResponse,
  JoinLobbyRequest,
  LobbyStateResponse
}
import io.github.pallax03.wizard.codecs.engine.lobby.LobbyCodecs.given

/**
 * Circe codecs and Tapir schemas for the lobby-specific HTTP request/response types.
 *
 * [[JoinLobbyRequest]], [[LobbyStateResponse]] and [[GameStartedResponse]] are
 * defined in the web layer but their serialization contract belongs here,
 * mirroring the engine codec structure.
 *
 * Depends on [[LobbyCodecs]] and [[PlayerIdCodecs]] for the engine sub-types
 * (Player, LobbyId, BotsDifficulty, PlayerId) they reference.
 */
object LobbyRequestCodecs:
  // --- Circe ---
  given Encoder[JoinLobbyRequest]    = deriveEncoder
  given Decoder[JoinLobbyRequest]    = deriveDecoder
  given Encoder[LobbyStateResponse]  = deriveEncoder
  given Decoder[LobbyStateResponse]  = deriveDecoder
  given Encoder[GameStartedResponse] = deriveEncoder
  given Decoder[GameStartedResponse] = deriveDecoder

  // --- Tapir Schemas ---
  given Schema[JoinLobbyRequest]    = Schema.derived
  given Schema[LobbyStateResponse]  = Schema.derived
  given Schema[GameStartedResponse] = Schema.derived
