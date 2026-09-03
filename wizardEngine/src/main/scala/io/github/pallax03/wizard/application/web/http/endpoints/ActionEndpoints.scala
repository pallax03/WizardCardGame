package io.github.pallax03.wizard.application.web.http.endpoints

import io.circe.generic.auto.*

import io.github.pallax03.wizard.application.web.http.HttpSupport.given
import io.github.pallax03.wizard.application.web.http.{
  ActionSuccessResponse,
  ErrorResponse,
  HttpSupport
}
import io.github.pallax03.wizard.codecs.engine.model.core.GameActionCodecs.given
import io.github.pallax03.wizard.engine.lobby.LobbyId
import io.github.pallax03.wizard.engine.model.basic.PlayerId
import io.github.pallax03.wizard.engine.model.basic.cards.Card
import io.github.pallax03.wizard.engine.model.core.GameAction

import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*

given Schema[Card] = Schema.derived

object ActionEndpoints:

  /** Shared base: typed lobby/player + JSON GameAction body. */
  private val base
      : Endpoint[Unit, (LobbyId, PlayerId, GameAction), ErrorResponse, ActionSuccessResponse, Any] =
    endpoint.post
      .in("api" / "lobby" / HttpSupport.lobbyIdPath / "player" / HttpSupport.playerIdPath)
      .tag("Game Action")
      .in(jsonBody[GameAction])
      .out(jsonBody[ActionSuccessResponse])
      .errorOut(HttpSupport.errorOutput)

  /** POST /api/lobby/{lobbyId}/player/{playerId}/choose — resolve trump color. */
  val chooseAction
      : Endpoint[Unit, (LobbyId, PlayerId, GameAction), ErrorResponse, ActionSuccessResponse, Any] =
    base
      .summary("Resolve trump")
      .description("Submits a ResolveTrumpColor action. Valid only in ChoosingTrump phase.")
      .in("choose")

  /** POST /api/lobby/{lobbyId}/player/{playerId}/place — place a bid. */
  val placeAction
      : Endpoint[Unit, (LobbyId, PlayerId, GameAction), ErrorResponse, ActionSuccessResponse, Any] =
    base
      .summary("Place bid")
      .description("Submits a PlaceBid action. Valid only in Bidding phase.")
      .in("place")

  /** POST /api/lobby/{lobbyId}/player/{playerId}/play — play a card. */
  val playAction
      : Endpoint[Unit, (LobbyId, PlayerId, GameAction), ErrorResponse, ActionSuccessResponse, Any] =
    base
      .summary("Play card")
      .description("Submits a PlayCard action. Valid only in Playing phase.")
      .in("play")
