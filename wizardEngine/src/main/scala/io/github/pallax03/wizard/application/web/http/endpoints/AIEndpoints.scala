package io.github.pallax03.wizard.application.web.http.endpoints

import io.circe.generic.auto.*

import io.github.pallax03.wizard.application.web.http.HttpSupport.given
import io.github.pallax03.wizard.application.web.http.{
  ActionSuccessResponse,
  ErrorResponse,
  HttpSupport
}
import io.github.pallax03.wizard.engine.lobby.LobbyId
import io.github.pallax03.wizard.engine.model.basic.PlayerId

import sttp.tapir.*
import sttp.tapir.json.circe.*

object AIEndpoints:

  /** Shared base for all AI hint endpoints: typed lobby/player + hint prefix. */
  private val base: Endpoint[Unit, (LobbyId, PlayerId), ErrorResponse, ActionSuccessResponse, Any] =
    endpoint.get
      .in("api" / "lobby" / HttpSupport.lobbyIdPath / "player" / HttpSupport.playerIdPath / "hint")
      .tag("AI Hint")
      .out(jsonBody[ActionSuccessResponse])
      .errorOut(HttpSupport.errorOutput)

  /** GET /api/lobby/{lobbyId}/player/{playerId}/hint/choose — best trump color. */
  val bestTrump: Endpoint[Unit, (LobbyId, PlayerId), ErrorResponse, ActionSuccessResponse, Any] =
    base
      .summary("AI hint: best trump")
      .description("Returns the AI-suggested trump color for the dealer in ChoosingTrump phase.")
      .in("choose")

  /** GET /api/lobby/{lobbyId}/player/{playerId}/hint/bid — best bid. */
  val bestBid: Endpoint[Unit, (LobbyId, PlayerId), ErrorResponse, ActionSuccessResponse, Any] =
    base
      .summary("AI hint: best bid")
      .description("Returns the AI-suggested bid for the current Bidding phase.")
      .in("bid")

  /** GET /api/lobby/{lobbyId}/player/{playerId}/hint/card — best card. */
  val bestCard: Endpoint[Unit, (LobbyId, PlayerId), ErrorResponse, ActionSuccessResponse, Any] =
    base
      .summary("AI hint: best card")
      .description("Returns the AI-suggested card to play for the current Playing phase.")
      .in("card")
