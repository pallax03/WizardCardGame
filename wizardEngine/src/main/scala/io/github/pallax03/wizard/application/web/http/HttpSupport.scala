package io.github.pallax03.wizard.application.web.http

import io.circe.generic.auto._
import io.github.pallax03.wizard.engine.lobby.LobbyId
import io.github.pallax03.wizard.engine.model.basic.PlayerId
import sttp.model.StatusCode
import sttp.tapir._
import sttp.tapir.generic.auto._
import sttp.tapir.json.circe._

/**
 * Shared HTTP protocol definitions for all Tapir endpoints.
 *
 * Centralizes error model, status-code mapping, typed path inputs
 * and Tapir schemas so every `*Endpoints` file follows the same contract.
 * All feature endpoints should reuse `lobbyIdPath`, `playerIdPath`
 * and `errorOutput` instead of redefining them locally.
 */
case class ErrorResponse(message: String, code: String)

case class ActionSuccessResponse(message: String)

object HttpSupport:

  given Schema[PlayerId] = Schema.string
  given Schema[LobbyId] = Schema.string
  given Schema[ErrorResponse] = Schema.derived
  given Schema[ActionSuccessResponse] = Schema.derived

  /** Typed path extractor for `/lobby/{lobbyId}`. */
  val lobbyIdPath: EndpointInput[LobbyId] =
    path[String]("lobbyId").map(LobbyId.apply)(_.toString)

  /** Typed path extractor for `/player/{playerId}`. */
  val playerIdPath: EndpointInput[PlayerId] =
    path[String]("playerId").map(s => PlayerId(s.toInt))(_.toInt.toString)

  /** Shared error output: maps [[ErrorResponse]] to 400 / 404 / 500 for Swagger. */
  val errorOutput: EndpointOutput[ErrorResponse] =
    oneOf(
      oneOfVariant(StatusCode.BadRequest, jsonBody[ErrorResponse]),
      oneOfVariant(StatusCode.NotFound, jsonBody[ErrorResponse]),
      oneOfVariant(StatusCode.InternalServerError, jsonBody[ErrorResponse])
    )
