package io.github.pallax03.wizard.application.web.http.endpoints

import io.circe.generic.auto._

import io.github.pallax03.wizard.engine.lobby.LobbyId
import io.github.pallax03.wizard.engine.model.basic.PlayerId

import sttp.model.StatusCode
import sttp.tapir._
import sttp.tapir.generic.auto._
import sttp.tapir.json.circe._

object AIEndpoints:

  private val lobbyIdPath = path[String]("lobbyId").map(s => LobbyId(s))(_.toString)
  private val playerIdPath = path[String]("playerId").map(s => PlayerId(s.toInt))(_.toString)

  private val baseAIEndpoint = endpoint.get
    .in("lobby" / lobbyIdPath / "player" / playerIdPath / "hint")
    .out(jsonBody[ActionSuccessResponse])
    .errorOut(
      oneOf(
        oneOfVariant(StatusCode.BadRequest, jsonBody[ErrorResponse]),
        oneOfVariant(StatusCode.NotFound, jsonBody[ErrorResponse]),
        oneOfVariant(StatusCode.InternalServerError, jsonBody[ErrorResponse])
      )
    )

  val bestTrump: Endpoint[Unit, (LobbyId, PlayerId), ErrorResponse, ActionSuccessResponse, Any] =
    baseAIEndpoint.in("choose")
  val bestBid: Endpoint[Unit, (LobbyId, PlayerId), ErrorResponse, ActionSuccessResponse, Any] =
    baseAIEndpoint.in("bid")
  val bestCard: Endpoint[Unit, (LobbyId, PlayerId), ErrorResponse, ActionSuccessResponse, Any] =
    baseAIEndpoint.in("card")
