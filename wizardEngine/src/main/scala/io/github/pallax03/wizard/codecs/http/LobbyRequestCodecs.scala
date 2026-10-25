package io.github.pallax03.wizard.codecs.http

import io.circe.generic.semiauto.*
import io.circe.{Decoder, Encoder}

import io.github.pallax03.wizard.application.web.http.{
  JoinLobbyRequest,
  LobbyStateResponse,
  PublicPlayerInfo
}
import io.github.pallax03.wizard.codecs.engine.lobby.LobbyCodecs.given
import io.github.pallax03.wizard.codecs.engine.model.basic.PlayerIdCodecs.given

import sttp.tapir.Schema
import sttp.tapir.generic.auto.*

/**
 * Circe codecs and Tapir schemas for lobby-specific HTTP request and response DTOs:
 *  - [[JoinLobbyRequest]]: player matchmaking payload.
 *  - [[PublicPlayerInfo]]: sanitized player representation for waiting rooms.
 *  - [[LobbyStateResponse]]: full public lobby overview.
 */
object LobbyRequestCodecs:
  given Encoder[JoinLobbyRequest] = deriveEncoder
  given Decoder[JoinLobbyRequest] = deriveDecoder
  given Encoder[PublicPlayerInfo] = deriveEncoder
  given Decoder[PublicPlayerInfo] = deriveDecoder
  given Encoder[LobbyStateResponse] = deriveEncoder
  given Decoder[LobbyStateResponse] = deriveDecoder

  given Schema[JoinLobbyRequest] = Schema.derived
  given Schema[PublicPlayerInfo] = Schema.derived
  given Schema[LobbyStateResponse] = Schema.derived
