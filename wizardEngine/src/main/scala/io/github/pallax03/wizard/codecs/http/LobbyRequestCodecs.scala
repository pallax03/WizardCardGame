package io.github.pallax03.wizard.codecs.http

import io.circe.generic.semiauto.*
import io.circe.{Decoder, Encoder}

import io.github.pallax03.wizard.application.web.http.endpoints.{
  GameStartedResponse,
  JoinLobbyRequest,
  LobbyStateResponse,
  PublicPlayerInfo
}
import io.github.pallax03.wizard.codecs.engine.lobby.LobbyCodecs.given
import io.github.pallax03.wizard.codecs.engine.model.basic.PlayerIdCodecs.given

import sttp.tapir.Schema
import sttp.tapir.generic.auto.*

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
  given Encoder[JoinLobbyRequest] = deriveEncoder
  given Decoder[JoinLobbyRequest] = deriveDecoder
  given Encoder[PublicPlayerInfo] = deriveEncoder
  given Decoder[PublicPlayerInfo] = deriveDecoder
  given Encoder[LobbyStateResponse] = deriveEncoder
  given Decoder[LobbyStateResponse] = deriveDecoder
  given Encoder[GameStartedResponse] = deriveEncoder
  given Decoder[GameStartedResponse] = deriveDecoder

  // --- Tapir Schemas ---
  given Schema[JoinLobbyRequest] = Schema.derived
  given Schema[PublicPlayerInfo] = Schema.derived
  given Schema[LobbyStateResponse] = Schema.derived
  given Schema[GameStartedResponse] = Schema.derived
