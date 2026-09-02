package io.github.pallax03.wizard.application.web.http.routes

import scala.concurrent.{ExecutionContext, Future}

import io.github.pallax03.wizard.application.web.http.endpoints.{
  AIEndpoints,
  ActionSuccessResponse,
  ErrorResponse
}
import io.github.pallax03.wizard.codecs.engine.model.basic.CardCodecs.given
import io.github.pallax03.wizard.codecs.syntax.CodecSyntax._
import io.github.pallax03.wizard.engine.lobby.LobbyId
import io.github.pallax03.wizard.engine.model.basic.PlayerId
import io.github.pallax03.wizard.engine.ports.{AIPort, LobbyStatePort}

import sttp.tapir.server.ServerEndpoint

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
