package io.github.pallax03.wizard.application.web.http.endpoints

import io.github.pallax03.wizard.application.web.http.{
  ActionSuccessResponse,
  HttpSupport
}
import io.github.pallax03.wizard.codecs.engine.model.basic.BiddingCodecs.given
import io.github.pallax03.wizard.codecs.engine.model.basic.CardCodecs.given
import io.github.pallax03.wizard.codecs.http.HttpCodecs.given
import io.github.pallax03.wizard.engine.lobby.LobbyId
import io.github.pallax03.wizard.engine.model.basic.bidding.Bid
import io.github.pallax03.wizard.engine.model.basic.cards.Card
import io.github.pallax03.wizard.engine.errors.AppError

import sttp.tapir.*
import sttp.tapir.json.circe.*

object ActionEndpoints:

  /** Shared base: typed lobby + Bearer token + JSON GameAction body. */
  private val base: Endpoint[String, LobbyId, AppError, ActionSuccessResponse, Any] =
    endpoint.post
      .in("api" / "lobby" / HttpSupport.lobbyIdPath)
      .tag("Game Action")
      .securityIn(auth.bearer[String]())
      .out(jsonBody[ActionSuccessResponse])
      .errorOut(HttpSupport.errorOutput)

  /** POST /api/lobby/{lobbyId}/choose — resolve trump color. */
  val chooseAction
      : Endpoint[String, (LobbyId, Card.Color), AppError, ActionSuccessResponse, Any] =
    base
      .summary("Resolve trump")
      .description("Submits a ResolveTrumpColor action. Valid only in ChoosingTrump phase.")
      .in("choose")
      .in(jsonBody[Card.Color].description("Trump color to resolve"))

  /** POST /api/lobby/{lobbyId}/place — place a bid. */
  val placeAction
      : Endpoint[String, (LobbyId, Bid), AppError, ActionSuccessResponse, Any] =
    base
      .summary("Place bid")
      .description("Submits a PlaceBid action. Valid only in Bidding phase.")
      .in("place")
      .in(jsonBody[Bid].description("Bid amount"))

  /** POST /api/lobby/{lobbyId}/play — play a card. */
  val playAction
      : Endpoint[String, (LobbyId, Card), AppError, ActionSuccessResponse, Any] =
    base
      .summary("Play card")
      .description("Submits a PlayCard action. Valid only in Playing phase.")
      .in("play")
      .in(jsonBody[Card].description("Card to play"))
