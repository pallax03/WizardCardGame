package it.unibo.pps.wizard.application.web.http.routes

import it.unibo.pps.wizard.application.web.http._
import it.unibo.pps.wizard.application.web.http.endpoints.ActionEndpoints
import it.unibo.pps.wizard.application.web.http.endpoints.ActionSuccessResponse
import it.unibo.pps.wizard.application.web.http.endpoints.ErrorResponse
import it.unibo.pps.wizard.engine.lobby.LobbyId
import it.unibo.pps.wizard.engine.model.core.GameAction
import it.unibo.pps.wizard.engine.ports.InboundPort
import it.unibo.pps.wizard.engine.ports.LobbyStatePort
import sttp.tapir.server.ServerEndpoint

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

class ActionRoutes(lobbyStatePort: LobbyStatePort, gameEnginePort: InboundPort)(using
    ec: ExecutionContext
):

  private def handleAction(
      lobbyIdStr: String,
      playerId: String,
      action: GameAction
  ): Future[Either[ErrorResponse, ActionSuccessResponse]] =
    val lobbyId = LobbyId(lobbyIdStr)
    lobbyStatePort
      .getLobby(lobbyId)
      .flatMap:
        case Some(lobby) =>
          gameEnginePort
            .submitAction(lobbyId, action)
            .map: _ =>
              Right(
                ActionSuccessResponse(
                  s"Action submitted successfully from player $playerId in lobby ${lobby.uuid}"
                )
              )
        case None =>
          Future.successful(Left(ErrorResponse(s"Lobby $lobbyIdStr not found", "LOBBY_NOT_FOUND")))
      .recover:
        case ex: Throwable =>
          Left(ErrorResponse(s"Internal error: ${ex.getMessage}", "INTERNAL_ERROR"))

  val chooseServerEndpoint: ServerEndpoint[Any, Future]{type SECURITY_INPUT = Unit; type PRINCIPAL = Unit; type INPUT = (String, String, GameAction); type ERROR_OUTPUT = ErrorResponse; type OUTPUT = ActionSuccessResponse} = ActionEndpoints.chooseAction
    .serverLogic:
      case (lobbyId, playerId, action) => handleAction(lobbyId, playerId, action)

  val placeServerEndpoint: ServerEndpoint[Any, Future]{type SECURITY_INPUT = Unit; type PRINCIPAL = Unit; type INPUT = (String, String, GameAction); type ERROR_OUTPUT = ErrorResponse; type OUTPUT = ActionSuccessResponse} = ActionEndpoints.placeAction
    .serverLogic:
      case (lobbyId, playerId, action) => handleAction(lobbyId, playerId, action)

  val playServerEndpoint: ServerEndpoint[Any, Future]{type SECURITY_INPUT = Unit; type PRINCIPAL = Unit; type INPUT = (String, String, GameAction); type ERROR_OUTPUT = ErrorResponse; type OUTPUT = ActionSuccessResponse} = ActionEndpoints.playAction
    .serverLogic:
      case (lobbyId, playerId, action) => handleAction(lobbyId, playerId, action)

  val all: List[ServerEndpoint[Any, Future]] = List(
    chooseServerEndpoint,
    placeServerEndpoint,
    playServerEndpoint
  )
