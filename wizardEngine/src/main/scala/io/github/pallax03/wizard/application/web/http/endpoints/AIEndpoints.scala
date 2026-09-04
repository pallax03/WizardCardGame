package io.github.pallax03.wizard.application.web.http.endpoints

import io.github.pallax03.wizard.application.web.http.{
  ActionSuccessResponse,
  HttpSupport
}
import io.github.pallax03.wizard.codecs.http.HttpCodecs.given
import io.github.pallax03.wizard.engine.lobby.LobbyId
import io.github.pallax03.wizard.engine.errors.AppError

import sttp.tapir.*
import sttp.tapir.json.circe.*

object AIEndpoints:

  /** Shared base for all AI hint endpoints: typed lobby + Bearer token + hint prefix. */
  private val base: Endpoint[String, LobbyId, AppError, ActionSuccessResponse, Any] =
    endpoint.get
      .in("api" / "lobby" / HttpSupport.lobbyIdPath / "hint")
      .tag("AI Hint")
      .securityIn(auth.bearer[String]())
      .out(jsonBody[ActionSuccessResponse])
      .errorOut(HttpSupport.errorOutput)

  /** GET /api/lobby/{lobbyId}/hint/choose — best trump color. */
  val bestTrump: Endpoint[String, LobbyId, AppError, ActionSuccessResponse, Any] =
    base
      .summary("AI hint: best trump")
      .description("Returns the AI-suggested trump color for the dealer in ChoosingTrump phase.")
      .in("choose")

  /** GET /api/lobby/{lobbyId}/hint/bid — best bid. */
  val bestBid: Endpoint[String, LobbyId, AppError, ActionSuccessResponse, Any] =
    base
      .summary("AI hint: best bid")
      .description("Returns the AI-suggested bid for the current Bidding phase.")
      .in("bid")

  /** GET /api/lobby/{lobbyId}/hint/card — best card. */
  val bestCard: Endpoint[String, LobbyId, AppError, ActionSuccessResponse, Any] =
    base
      .summary("AI hint: best card")
      .description("Returns the AI-suggested card to play for the current Playing phase.")
      .in("card")
