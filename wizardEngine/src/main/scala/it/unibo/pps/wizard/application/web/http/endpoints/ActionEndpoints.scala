package it.unibo.pps.wizard.application.web.http.endpoints

import io.circe.generic.auto.*
import it.unibo.pps.wizard.codecs.engine.model.core.GameActionCodecs.given
import it.unibo.pps.wizard.engine.model.basic.cards.Card
import it.unibo.pps.wizard.engine.model.core.GameAction
import sttp.model.StatusCode
import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*

case class ActionSuccessResponse(message: String)
given Schema[Card] = Schema.derived

object ActionEndpoints:

  private val baseActionEndpoint = endpoint
    .post
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

  val chooseAction = baseActionEndpoint.in("choose")
  val placeAction = baseActionEndpoint.in("place")
  val playAction = baseActionEndpoint.in("play")
