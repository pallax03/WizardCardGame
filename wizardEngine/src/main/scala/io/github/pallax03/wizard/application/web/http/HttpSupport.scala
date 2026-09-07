package io.github.pallax03.wizard.application.web.http

import io.github.pallax03.wizard.codecs.http.AppErrorCodecs.given
import io.github.pallax03.wizard.engine.errors.AppError
import io.github.pallax03.wizard.engine.lobby.LobbyId
import io.github.pallax03.wizard.engine.model.basic.PlayerId

import sttp.model.StatusCode
import sttp.tapir.*
import sttp.tapir.json.circe.*

case class ActionSuccessResponse(message: String)

case class AuthLobbyPlayer(lobbyId: LobbyId, playerId: PlayerId, secret: Option[String] = None)
case class LobbyPlayer(lobbyId: LobbyId, playerId: PlayerId)

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

  /** Shared error output: maps [[AppError]] to 400 / 401 / 404 / 500 for Swagger. */
  val errorOutput: EndpointOutput[AppError] =
    oneOf[AppError](
      oneOfVariantValueMatcher(StatusCode.NotFound, jsonBody[AppError]) {
        case _: AppError.NotFoundError => true
      },
      oneOfVariantValueMatcher(StatusCode.Unauthorized, jsonBody[AppError]) {
        case _: AppError.UnauthorizedError => true
      },
      oneOfVariantValueMatcher(StatusCode.BadRequest, jsonBody[AppError]) {
        case _: AppError.BadRequestError => true
      },
      oneOfVariantValueMatcher(StatusCode.InternalServerError, jsonBody[AppError]) {
        case _: AppError.InternalError => true
      }
    )
