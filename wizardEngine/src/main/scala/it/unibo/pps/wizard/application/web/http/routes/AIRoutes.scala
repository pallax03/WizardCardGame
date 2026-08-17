package it.unibo.pps.wizard.application.web.http.routes

import it.unibo.pps.wizard.application.web.http.endpoints.AIEndpoints
import it.unibo.pps.wizard.application.web.http.endpoints.ActionSuccessResponse
import it.unibo.pps.wizard.application.web.http.endpoints.ErrorResponse
import it.unibo.pps.wizard.codecs.engine.model.basic.CardCodecs.given
import it.unibo.pps.wizard.codecs.syntax.CodecSyntax._
import it.unibo.pps.wizard.engine.lobby.LobbyId
import it.unibo.pps.wizard.engine.model.basic.PlayerId
import it.unibo.pps.wizard.engine.ports.AIPort
import it.unibo.pps.wizard.engine.ports.LobbyStatePort
import sttp.tapir.server.ServerEndpoint

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

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
          Future.successful(
            Left(ErrorResponse(s"Lobby ${lobbyId.toString} not found", "LOBBY_NOT_FOUND"))
          )
      .recover:
        case ex: Throwable =>
          Left(ErrorResponse(s"Internal error: ${ex.getMessage}", "INTERNAL_ERROR"))

  val hintBestTrump: ServerEndpoint[Any, Future] {
    type SECURITY_INPUT = Unit; type PRINCIPAL = Unit; type INPUT = (LobbyId, PlayerId);
    type ERROR_OUTPUT = ErrorResponse; type OUTPUT = ActionSuccessResponse
  } = AIEndpoints.bestTrump
    .serverLogic: (lobbyId, playerId) =>
      handleAction(lobbyId, aiPort.resolvedTrumpColor(lobbyId, playerId), _.toJson)

  val hintBestBid: ServerEndpoint[Any, Future] {
    type SECURITY_INPUT = Unit; type PRINCIPAL = Unit; type INPUT = (LobbyId, PlayerId);
    type ERROR_OUTPUT = ErrorResponse; type OUTPUT = ActionSuccessResponse
  } = AIEndpoints.bestBid
    .serverLogic: (lobbyId, playerId) =>
      handleAction(lobbyId, aiPort.placeBid(lobbyId, playerId), _.toJson)

  val hintBestCard: ServerEndpoint[Any, Future] {
    type SECURITY_INPUT = Unit; type PRINCIPAL = Unit; type INPUT = (LobbyId, PlayerId);
    type ERROR_OUTPUT = ErrorResponse; type OUTPUT = ActionSuccessResponse
  } = AIEndpoints.bestCard
    .serverLogic: (lobbyId, playerId) =>
      handleAction(lobbyId, aiPort.bestCard(lobbyId, playerId), _.toJson)

  val all: List[ServerEndpoint[Any, Future]] = List(
    hintBestTrump,
    hintBestBid,
    hintBestCard
  )
