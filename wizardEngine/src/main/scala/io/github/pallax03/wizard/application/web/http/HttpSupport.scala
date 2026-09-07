package io.github.pallax03.wizard.application.web.http

import io.github.pallax03.wizard.engine.lobby.LobbyId
import io.github.pallax03.wizard.engine.model.basic.PlayerId

import io.github.pallax03.wizard.engine.lobby.LobbyError
import io.github.pallax03.wizard.codecs.engine.lobby.LobbyCodecs.given

import sttp.model.StatusCode
import sttp.tapir.*
import sttp.tapir.json.circe.*

/**
 * Shared HTTP protocol definitions for all Tapir endpoints.
 *
 * Centralizes error model, status-code mapping, and typed path inputs
 * so every `*Endpoints` file follows the same contract.
 * All feature endpoints should reuse `lobbyIdPath`, `playerIdPath`
 * and `errorOutput` instead of redefining them locally.
 *
 * Serialization contracts (Circe codecs + Tapir schemas) for the types
 * defined here live in [[io.github.pallax03.wizard.codecs.http.HttpCodecs]].
 */
object HttpSupport:

  /** Typed path extractor for `/lobby/{lobbyId}`. */
  val lobbyIdPath: EndpointInput[LobbyId] =
    path[String]("lobbyId").map(LobbyId.apply)(_.toString)

  /** Typed path extractor for `/player/{playerId}`. */
  val playerIdPath: EndpointInput[PlayerId] =
    path[String]("playerId").map(s => PlayerId(s.toInt))(_.toInt.toString)

  /** Shared error output: maps [[LobbyError]] to 400 / 401 / 404 for Swagger. */
  val errorOutput: EndpointOutput[LobbyError] =
    oneOf[LobbyError](
      oneOfVariantValueMatcher(StatusCode.NotFound, jsonBody[LobbyError]) {
        case LobbyError.PlayerNotFound | LobbyError.LobbyNotFound => true
      },
      oneOfVariantValueMatcher(StatusCode.Unauthorized, jsonBody[LobbyError]) {
        case LobbyError.NotAuthenticated => true
      },
      oneOfVariantValueMatcher(StatusCode.BadRequest, jsonBody[LobbyError]) {
        case _ => true
      }
    )
