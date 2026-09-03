package io.github.pallax03.wizard.application.web.http.routes

import scala.concurrent.{ExecutionContext, Future}

import io.github.pallax03.wizard.application.web.http.endpoints.ActionEndpoints
import io.github.pallax03.wizard.application.web.http.{ActionSuccessResponse, ErrorResponse}
import io.github.pallax03.wizard.engine.lobby.LobbyId
import io.github.pallax03.wizard.engine.model.basic.PlayerId
import io.github.pallax03.wizard.engine.model.core.GameAction
import io.github.pallax03.wizard.engine.ports.{InboundPort, LobbyStatePort}

import sttp.tapir.server.ServerEndpoint

/**
 * HTTP routes for Game Action domain.
 *
 * Standard layout:
 *   1. `handleAction` helper: lobby existence check + action submission
 *   2. validates that `action.playerId` matches the `playerId` path param
 *   3. `ServerEndpoint` vals for choose/place/play
 *   4. `all` aggregation
 */
class ActionRoutes(lobbyStatePort: LobbyStatePort, gameEnginePort: InboundPort)(using
    ec: ExecutionContext
):

  private def handleAction(
      lobbyId: LobbyId,
      playerId: PlayerId,
      action: GameAction
  ): Future[Either[ErrorResponse, ActionSuccessResponse]] =
    if action.playerId != playerId then
      Future.successful(
        Left(
          ErrorResponse(
            s"playerId mismatch: path $playerId vs body ${action.playerId}",
            "BAD_REQUEST"
          )
        )
      )
    else
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
            Future.successful(Left(ErrorResponse(s"Lobby $lobbyId not found", "LOBBY_NOT_FOUND")))

  val chooseEndpoint: ServerEndpoint[Any, Future] =
    ActionEndpoints.chooseAction.serverLogic { case (lobbyId, playerId, action) =>
      handleAction(lobbyId, playerId, action)
    }

  val placeEndpoint: ServerEndpoint[Any, Future] =
    ActionEndpoints.placeAction.serverLogic { case (lobbyId, playerId, action) =>
      handleAction(lobbyId, playerId, action)
    }

  val playEndpoint: ServerEndpoint[Any, Future] =
    ActionEndpoints.playAction.serverLogic { case (lobbyId, playerId, action) =>
      handleAction(lobbyId, playerId, action)
    }

  val all: List[ServerEndpoint[Any, Future]] = List(
    chooseEndpoint,
    placeEndpoint,
    playEndpoint
  )
