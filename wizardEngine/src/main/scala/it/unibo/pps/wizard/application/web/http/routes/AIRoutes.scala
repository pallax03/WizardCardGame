package it.unibo.pps.wizard.application.web.http.routes

import it.unibo.pps.wizard.application.web.http.endpoints.{AIEndpoints, ActionSuccessResponse, ErrorResponse}
import it.unibo.pps.wizard.engine.lobby.LobbyId
import it.unibo.pps.wizard.engine.ports.{AIPort, LobbyStatePort}
import it.unibo.pps.wizard.codecs.engine.model.basic.CardCodecs.given
import it.unibo.pps.wizard.codecs.syntax.CodecSyntax.*
import sttp.tapir.server.ServerEndpoint

import scala.concurrent.{ExecutionContext, Future}


class AIRoutes(lobbyStatePort: LobbyStatePort, aiPort: AIPort)(using ec: ExecutionContext):

  private def handleAction[A](
                               lobbyId: LobbyId,
                             action: => Future[A],
                             callback: A => String
                             ): Future[Either[ErrorResponse, ActionSuccessResponse]] =
    lobbyStatePort
      .getLobby(lobbyId)
      .flatMap:
        case Some(_) =>
          action.map: res => 
            Right(ActionSuccessResponse(callback(res)))
        case None =>
          Future.successful(Left(ErrorResponse(s"Lobby ${lobbyId.toString} not found", "LOBBY_NOT_FOUND")))
      .recover:
        case ex: Throwable =>
          Left(ErrorResponse(s"Internal error: ${ex.getMessage}", "INTERNAL_ERROR"))

  val hintBestTrump = AIEndpoints.bestTrump
    .serverLogic: (lobbyId, playerId) =>
      handleAction(lobbyId, aiPort.resolvedTrumpColor(lobbyId, playerId), _.toJson)

  val hintBestBid = AIEndpoints.bestBid
    .serverLogic: (lobbyId, playerId) =>
      handleAction(lobbyId, aiPort.placeBid(lobbyId, playerId), _.toJson)

  val hintBestCard = AIEndpoints.bestCard
    .serverLogic: (lobbyId, playerId) =>
      handleAction(lobbyId, aiPort.bestCard(lobbyId, playerId), _.toJson)

  val all: List[ServerEndpoint[Any, Future]] = List(
    hintBestTrump,
    hintBestBid,
    hintBestCard
  )