package io.github.pallax03.wizard.application.web.http.endpoints

import io.github.pallax03.wizard.application.web.http.{
  ActionSuccessResponse,
  ErrorResponse,
  HttpSupport
}
import io.github.pallax03.wizard.codecs.engine.model.basic.BiddingCodecs.given
import io.github.pallax03.wizard.codecs.engine.model.basic.CardCodecs.given
import io.github.pallax03.wizard.codecs.http.HttpCodecs.given
import io.github.pallax03.wizard.engine.lobby.LobbyId
import io.github.pallax03.wizard.engine.model.basic.PlayerId
import io.github.pallax03.wizard.engine.model.basic.bidding.Bid
import io.github.pallax03.wizard.engine.model.basic.cards.Card

import sttp.tapir.*
import sttp.tapir.json.circe.*

object ActionEndpoints:

  /** Shared base: typed lobby/player + JSON GameAction body. */
  private val base: Endpoint[Unit, (LobbyId, PlayerId), ErrorResponse, ActionSuccessResponse, Any] =
    endpoint.post
      .in("api" / "lobby" / HttpSupport.lobbyIdPath / "player" / HttpSupport.playerIdPath)
      .tag("Game Action")
      .out(jsonBody[ActionSuccessResponse])
      .errorOut(HttpSupport.errorOutput)

  /** POST /api/lobby/{lobbyId}/player/{playerId}/choose — resolve trump color. */
  val chooseAction
      : Endpoint[Unit, (LobbyId, PlayerId, Card.Color), ErrorResponse, ActionSuccessResponse, Any] =
    base
      .summary("Resolve trump")
      .description("Submits a ResolveTrumpColor action. Valid only in ChoosingTrump phase.")
      .in("choose")
      .in(jsonBody[Card.Color].description("Trump color to resolve"))

  /** POST /api/lobby/{lobbyId}/player/{playerId}/place — place a bid. */
  val placeAction
      : Endpoint[Unit, (LobbyId, PlayerId, Bid), ErrorResponse, ActionSuccessResponse, Any] =
    base
      .summary("Place bid")
      .description("Submits a PlaceBid action. Valid only in Bidding phase.")
      .in("place")
      .in(jsonBody[Bid].description("Bid amount"))

  /** POST /api/lobby/{lobbyId}/player/{playerId}/play — play a card. */
  val playAction
      : Endpoint[Unit, (LobbyId, PlayerId, Card), ErrorResponse, ActionSuccessResponse, Any] =
    base
      .summary("Play card")
      .description("Submits a PlayCard action. Valid only in Playing phase.")
      .in("play")
      .in(jsonBody[Card].description("Card to play"))
