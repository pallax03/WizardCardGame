package io.github.pallax03.wizard.application.web.http.routes

import scala.concurrent.{ExecutionContext, Future}

import io.github.pallax03.wizard.application.web.http.ActionSuccessResponse
import io.github.pallax03.wizard.application.web.http.endpoints.ActionEndpoints
import io.github.pallax03.wizard.engine.errors.AppError
import io.github.pallax03.wizard.engine.lobby.LobbyId
import io.github.pallax03.wizard.engine.model.basic.PlayerId
import io.github.pallax03.wizard.engine.model.core.GameAction
import io.github.pallax03.wizard.engine.model.core.GameAction.PlayCard
import io.github.pallax03.wizard.engine.ports.{InboundPort, LobbyStatePort}

import sttp.tapir.server.ServerEndpoint

class ActionRoutes(lobbyStatePort: LobbyStatePort, gameEnginePort: InboundPort)(using
    ec: ExecutionContext
):

  private def handleAction(
      secret: String,
      lobbyId: LobbyId,
      actionBuilder: PlayerId => GameAction
  ): Future[Either[AppError, ActionSuccessResponse]] =
    lobbyStatePort
      .getLobby(lobbyId)
      .flatMap:
        case Some(lobby) =>
          lobby.players.find(_.secret.contains(secret)) match
            case Some(player) =>
              gameEnginePort
                .submitAction(lobbyId, actionBuilder(player.id))
                .map:
                  case Left(gameError) => Left(AppError.GameError(gameError.toString))
                  case Right(_) =>
                    Right(
                      ActionSuccessResponse(
                        s"Action submitted successfully from player ${player.id} in lobby ${lobby.uuid}"
                      )
                    )
            case None =>
              Future.successful(Left(AppError.NotAuthenticated))
        case None =>
          Future.successful(Left(AppError.LobbyNotFound(lobbyId)))

  private val chooseEndpoint: ServerEndpoint[Any, Future] =
    ActionEndpoints.chooseAction
      .serverSecurityLogicSuccess(secret => Future.successful(secret))
      .serverLogic(secret => { case (lobbyId, trumpColor) =>
        handleAction(
          secret,
          lobbyId,
          playerId => GameAction.ResolveTrumpColor(playerId, trumpColor)
        )
      })

  private val placeEndpoint: ServerEndpoint[Any, Future] =
    ActionEndpoints.placeAction
      .serverSecurityLogicSuccess(secret => Future.successful(secret))
      .serverLogic(secret => { case (lobbyId, bid) =>
        handleAction(secret, lobbyId, playerId => GameAction.PlaceBid(playerId, bid))
      })

  private val playEndpoint: ServerEndpoint[Any, Future] =
    ActionEndpoints.playAction
      .serverSecurityLogicSuccess(secret => Future.successful(secret))
      .serverLogic(secret => { case (lobbyId, card) =>
        handleAction(secret, lobbyId, playerId => PlayCard(playerId, card))
      })

  val all: List[ServerEndpoint[Any, Future]] = List(
    chooseEndpoint,
    placeEndpoint,
    playEndpoint
  )
