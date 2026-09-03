package io.github.pallax03.wizard.application.web.http.routes

import io.github.pallax03.wizard.application.web.http.ActionSuccessResponse
import io.github.pallax03.wizard.application.web.http.ErrorResponse
import io.github.pallax03.wizard.application.web.http.endpoints.AIEndpoints
import io.github.pallax03.wizard.codecs.engine.model.basic.CardCodecs.given
import io.github.pallax03.wizard.codecs.syntax.CodecSyntax._
import io.github.pallax03.wizard.engine.lobby.LobbyId
import io.github.pallax03.wizard.engine.ports.AIPort
import io.github.pallax03.wizard.engine.ports.LobbyStatePort
import sttp.tapir.server.ServerEndpoint

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

/**
 * HTTP routes for AI hint domain.
 *
 * Standard layout:
 *   1. `handleHint` helper: validates lobby existence, runs AI computation, maps errors
 *   2. `ServerEndpoint` vals delegating to `AIEndpoints.*`
 *   3. `all` aggregation
 */
class AIRoutes(lobbyStatePort: LobbyStatePort, aiPort: AIPort)(using ec: ExecutionContext):

  private def handleHint[A](
      lobbyId: LobbyId,
      action: => Future[A],
      encode: A => String
  ): Future[Either[ErrorResponse, ActionSuccessResponse]] =
    lobbyStatePort
      .getLobby(lobbyId)
      .flatMap:
        case Some(_) =>
          action.map(res => Right(ActionSuccessResponse(encode(res))))
        case None =>
          Future.successful(
            Left(ErrorResponse(s"Lobby $lobbyId not found", "LOBBY_NOT_FOUND"))
          )
      .recover:
        case ex: Throwable =>
          Left(ErrorResponse(s"Internal error: ${ex.getMessage}", "INTERNAL_ERROR"))

  val hintBestTrump: ServerEndpoint[Any, Future] =
    AIEndpoints.bestTrump.serverLogic { case (lobbyId, playerId) =>
      handleHint(lobbyId, aiPort.resolvedTrumpColor(lobbyId, playerId), _.toJson)
    }

  val hintBestBid: ServerEndpoint[Any, Future] =
    AIEndpoints.bestBid.serverLogic { case (lobbyId, playerId) =>
      handleHint(lobbyId, aiPort.placeBid(lobbyId, playerId), _.toJson)
    }

  val hintBestCard: ServerEndpoint[Any, Future] =
    AIEndpoints.bestCard.serverLogic { case (lobbyId, playerId) =>
      handleHint(lobbyId, aiPort.bestCard(lobbyId, playerId), _.toJson)
    }

  val all: List[ServerEndpoint[Any, Future]] = List(
    hintBestTrump,
    hintBestBid,
    hintBestCard
  )
