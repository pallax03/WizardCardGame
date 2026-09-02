package io.github.pallax03.wizard.application.web.http.routes

import scala.concurrent.{ExecutionContext, Future}

import io.github.pallax03.wizard.application.web.http._
import io.github.pallax03.wizard.application.web.http.endpoints.{
  ActionEndpoints,
  ActionSuccessResponse,
  ErrorResponse
}
import io.github.pallax03.wizard.engine.lobby.LobbyId
import io.github.pallax03.wizard.engine.model.core.GameAction
import io.github.pallax03.wizard.engine.ports.{InboundPort, LobbyStatePort}

import sttp.tapir.server.ServerEndpoint

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
            .map:
              case Right(_) =>
                Right(
                  ActionSuccessResponse(
                    s"Action submitted successfully from player $playerId in lobby ${lobby.uuid}"
                  )
                )
              case Left(gameError) =>
                Left(ErrorResponse(gameError.toString, "INVALID_ACTION"))
        case None =>
          Future.successful(Left(ErrorResponse(s"Lobby $lobbyIdStr not found", "LOBBY_NOT_FOUND")))

  val chooseServerEndpoint: ServerEndpoint[Any, Future] {
    type SECURITY_INPUT = Unit; type PRINCIPAL = Unit; type INPUT = (String, String, GameAction);
    type ERROR_OUTPUT = ErrorResponse; type OUTPUT = ActionSuccessResponse
  } = ActionEndpoints.chooseAction
    .serverLogic:
      case (lobbyId, playerId, action) => handleAction(lobbyId, playerId, action)

  val placeServerEndpoint: ServerEndpoint[Any, Future] {
    type SECURITY_INPUT = Unit; type PRINCIPAL = Unit; type INPUT = (String, String, GameAction);
    type ERROR_OUTPUT = ErrorResponse; type OUTPUT = ActionSuccessResponse
  } = ActionEndpoints.placeAction
    .serverLogic:
      case (lobbyId, playerId, action) => handleAction(lobbyId, playerId, action)

  val playServerEndpoint: ServerEndpoint[Any, Future] {
    type SECURITY_INPUT = Unit; type PRINCIPAL = Unit; type INPUT = (String, String, GameAction);
    type ERROR_OUTPUT = ErrorResponse; type OUTPUT = ActionSuccessResponse
  } = ActionEndpoints.playAction
    .serverLogic:
      case (lobbyId, playerId, action) => handleAction(lobbyId, playerId, action)

  val all: List[ServerEndpoint[Any, Future]] = List(
    chooseServerEndpoint,
    placeServerEndpoint,
    playServerEndpoint
  )
