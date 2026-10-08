package io.github.pallax03.wizard.application.web.http.endpoints

import io.github.pallax03.wizard.application.web.http.HttpSupport
import io.github.pallax03.wizard.codecs.engine.model.basic.BiddingCodecs.given
import io.github.pallax03.wizard.codecs.engine.model.basic.CardCodecs.given
import io.github.pallax03.wizard.engine.lobby.{LobbyError, LobbyId}
import io.github.pallax03.wizard.engine.model.basic.bidding.Bid
import io.github.pallax03.wizard.engine.model.basic.cards.Card

import sttp.model.StatusCode
import sttp.tapir.*
import sttp.tapir.json.circe.*

/**
 * Declarative Tapir endpoint contracts for player game actions (`/api/lobby/{lobbyId}/...`).
 *
 * Defines mutating HTTP endpoints for player moves during active game phases:
 *  - Resolving trump color ([[chooseAction]]) during the trump selection phase.
 *  - Submitting round trick bids ([[placeAction]]) during the bidding phase.
 *  - Playing cards to the trick table ([[playAction]]) during the trick playing phase.
 *
 * All endpoints require Bearer authentication containing the acting player's secret token,
 * return HTTP 200 OK on successful state advancement, or propagate domain validation errors
 * through [[HttpSupport.errorOutput]].
 */
object ActionEndpoints:

  /** Shared base: typed lobby + Bearer token. */
  private val base: Endpoint[String, LobbyId, LobbyError, Unit, Any] =
    endpoint.post
      .in("api" / "lobby" / HttpSupport.lobbyIdPath)
      .tag("Game Action")
      .securityIn(auth.bearer[String]())
      .out(statusCode(StatusCode.Ok))
      .errorOut(HttpSupport.errorOutput)

  /** POST /api/lobby/{lobbyId}/choose — resolve trump color. */
  val chooseAction: Endpoint[String, (LobbyId, Card.Color), LobbyError, Unit, Any] =
    base
      .summary("Resolve trump")
      .description("Submits a ResolveTrumpColor action. Valid only in ChoosingTrump phase.")
      .in("choose")
      .in(jsonBody[Card.Color].description("Trump color to resolve"))

  /** POST /api/lobby/{lobbyId}/place — place a bid. */
  val placeAction: Endpoint[String, (LobbyId, Bid), LobbyError, Unit, Any] =
    base
      .summary("Place bid")
      .description("Submits a PlaceBid action. Valid only in Bidding phase.")
      .in("place")
      .in(jsonBody[Bid].description("Bid amount"))

  /** POST /api/lobby/{lobbyId}/play — play a card. */
  val playAction: Endpoint[String, (LobbyId, Card), LobbyError, Unit, Any] =
    base
      .summary("Play card")
      .description("Submits a PlayCard action. Valid only in Playing phase.")
      .in("play")
      .in(jsonBody[Card].description("Card to play"))
