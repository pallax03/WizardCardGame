package it.unibo.pps.wizard.application.web.http.endpoints

import io.circe.generic.auto.*
import it.unibo.pps.wizard.engine.lobby.LobbyId
import it.unibo.pps.wizard.engine.model.basic.PlayerId
import sttp.model.StatusCode
import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*

object AIEndpoints:

  private val lobbyIdPath = path[String]("lobbyId").map(s => LobbyId(s))(_.toString)
  private val playerIdPath = path[String]("playerId").map(s => PlayerId(s.toInt))(_.toString)

  private val baseAIEndpoint = endpoint
    .get
    .in("lobby" / lobbyIdPath / "player" / playerIdPath / "hint")
    .out(jsonBody[ActionSuccessResponse])
    .errorOut(
      oneOf(
        oneOfVariant(StatusCode.BadRequest, jsonBody[ErrorResponse]),
        oneOfVariant(StatusCode.NotFound, jsonBody[ErrorResponse]),
        oneOfVariant(StatusCode.InternalServerError, jsonBody[ErrorResponse])
      )
    )

  val bestTrump = baseAIEndpoint.in("choose")
  val bestBid = baseAIEndpoint.in("bid")
  val bestCard = baseAIEndpoint.in("card")
