package io.github.pallax03.wizard.application.web.http.endpoints

import io.github.pallax03.wizard.application.web.http.HttpSupport
import io.github.pallax03.wizard.codecs.engine.model.basic.BiddingCodecs.given
import io.github.pallax03.wizard.codecs.engine.model.basic.CardCodecs.given
import io.github.pallax03.wizard.engine.lobby.{LobbyError, LobbyId}
import io.github.pallax03.wizard.engine.model.basic.bidding.Bid
import io.github.pallax03.wizard.engine.model.basic.cards.Card

import sttp.tapir.*
import sttp.tapir.json.circe.*

object AIEndpoints:

  /** Shared base for all AI hint endpoints: typed lobby + Bearer token + hint prefix. */
  private val base =
    endpoint.get
      .in("api" / "lobby" / HttpSupport.lobbyIdPath / "hint")
      .tag("AI Hint")
      .securityIn(auth.bearer[String]())
      .errorOut(HttpSupport.errorOutput)

  /** GET /api/lobby/{lobbyId}/hint/choose — best trump color. */
  val bestTrump: Endpoint[String, LobbyId, LobbyError, Card.Color, Any] =
    base
      .out(jsonBody[Card.Color])
      .summary("AI hint: best trump")
      .description("Returns the AI-suggested trump color for the dealer in ChoosingTrump phase.")
      .in("choose")

  /** GET /api/lobby/{lobbyId}/hint/bid — best bid. */
  val bestBid: Endpoint[String, LobbyId, LobbyError, Bid, Any] =
    base
      .out(jsonBody[Bid])
      .summary("AI hint: best bid")
      .description("Returns the AI-suggested bid for the current Bidding phase.")
      .in("bid")

  /** GET /api/lobby/{lobbyId}/hint/card — best card. */
  val bestCard: Endpoint[String, LobbyId, LobbyError, Card, Any] =
    base
      .out(jsonBody[Card])
      .summary("AI hint: best card")
      .description("Returns the AI-suggested card to play for the current Playing phase.")
      .in("card")
