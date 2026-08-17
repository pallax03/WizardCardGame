package it.unibo.pps.wizard.application.web.http.routes

import it.unibo.pps.wizard.application.web.http.*
import it.unibo.pps.wizard.application.web.http.endpoints.{ActionEndpoints, ActionSuccessResponse, ErrorResponse}
import it.unibo.pps.wizard.engine.lobby.LobbyId
import it.unibo.pps.wizard.engine.model.core.GameAction
import it.unibo.pps.wizard.engine.ports.InboundPort
import it.unibo.pps.wizard.engine.ports.LobbyStatePort
import sttp.tapir.server.ServerEndpoint

import scala.concurrent.{ExecutionContext, Future}

class ActionRoutes(lobbyStatePort: LobbyStatePort, gameEnginePort: InboundPort)(using ec: ExecutionContext):

  private def handleAction(lobbyIdStr: String, playerId: String, action: GameAction): Future[Either[ErrorResponse, ActionSuccessResponse]] =
    val lobbyId = LobbyId(lobbyIdStr)
    lobbyStatePort
      .getLobby(lobbyId)
      .flatMap:
        case Some(lobby) =>
          gameEnginePort.submitAction(lobbyId, action).map: _ =>
            Right(ActionSuccessResponse(s"Action submitted successfully from player $playerId in lobby ${lobby.uuid}"))
        case None =>
          Future.successful(Left(ErrorResponse(s"Lobby $lobbyIdStr not found", "LOBBY_NOT_FOUND")))
      .recover:
        case ex: Throwable =>
          Left(ErrorResponse(s"Internal error: ${ex.getMessage}", "INTERNAL_ERROR"))

  val chooseServerEndpoint = ActionEndpoints.chooseAction
    .serverLogic:
      case (lobbyId, playerId, action) => handleAction(lobbyId, playerId, action)

  val placeServerEndpoint = ActionEndpoints.placeAction
    .serverLogic:
      case (lobbyId, playerId, action) => handleAction(lobbyId, playerId, action)

  val playServerEndpoint = ActionEndpoints.playAction
    .serverLogic:
      case (lobbyId, playerId, action) => handleAction(lobbyId, playerId, action)

  val all: List[ServerEndpoint[Any, Future]] = List(
    chooseServerEndpoint,
    placeServerEndpoint,
    playServerEndpoint
  )
