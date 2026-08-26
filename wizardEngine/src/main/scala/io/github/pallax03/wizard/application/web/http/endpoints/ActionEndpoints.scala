package io.github.pallax03.wizard.application.web.http.endpoints

import io.circe.generic.auto._
import io.github.pallax03.wizard.codecs.engine.model.core.GameActionCodecs.given
import io.github.pallax03.wizard.engine.model.basic.cards.Card
import io.github.pallax03.wizard.engine.model.core.GameAction
import sttp.model.StatusCode
import sttp.tapir._
import sttp.tapir.generic.auto._
import sttp.tapir.json.circe._

case class ActionSuccessResponse(message: String)
given Schema[Card] = Schema.derived

object ActionEndpoints:

  private val baseActionEndpoint = endpoint.post
    .in("lobby" / path[String]("lobbyId") / "player" / path[String]("playerId"))
    .in(jsonBody[GameAction])
    .out(jsonBody[ActionSuccessResponse])
    .errorOut(
      oneOf(
        oneOfVariant(StatusCode.BadRequest, jsonBody[ErrorResponse]),
        oneOfVariant(StatusCode.NotFound, jsonBody[ErrorResponse]),
        oneOfVariant(StatusCode.InternalServerError, jsonBody[ErrorResponse])
      )
    )

  val chooseAction
      : Endpoint[Unit, (String, String, GameAction), ErrorResponse, ActionSuccessResponse, Any] =
    baseActionEndpoint.in("choose")
  val placeAction
      : Endpoint[Unit, (String, String, GameAction), ErrorResponse, ActionSuccessResponse, Any] =
    baseActionEndpoint.in("place")
  val playAction
      : Endpoint[Unit, (String, String, GameAction), ErrorResponse, ActionSuccessResponse, Any] =
    baseActionEndpoint.in("play")
