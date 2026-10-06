package io.github.pallax03.wizard.application.web.http.routes

import scala.concurrent.{ExecutionContext, Future}

import io.github.pallax03.wizard.application.web.http.endpoints.ActionEndpoints
import io.github.pallax03.wizard.engine.lobby.{LobbyError, LobbyId, LobbyStatus}
import io.github.pallax03.wizard.engine.model.basic.PlayerId
import io.github.pallax03.wizard.engine.model.core.GameAction
import io.github.pallax03.wizard.engine.model.core.GameAction.PlayCard
import io.github.pallax03.wizard.engine.ports.{InboundPort, LobbyStatePort}

import sttp.tapir.server.ServerEndpoint

/**
 * Tapir HTTP server routes connecting game action endpoints ([[ActionEndpoints]]) to the core engine ([[InboundPort]]).
 *
 * Implements the action submission pipeline for interactive moves (trump resolution, bidding, card play):
 *  1. Authenticates the player's secret token and verifies membership in the specified lobby.
 *  2. Enforces active game state invariants: rejects moves when the lobby is not in [[LobbyStatus.IN_GAME]]
 *     (returning [[LobbyError.GamePaused]]).
 *  3. Submits the action to [[InboundPort.submitAction]], mapping engine rule violations to
 *     [[LobbyError.GameActionRejected]].
 *  4. Resets AFK strikes: upon a successful move, clears any accumulated inactivity strikes via
 *     [[LobbyStatePort.clearPlayerStrikes]].
 *
 * Exposes all endpoints via [[all]] for registration into [[HttpServerVerticle]].
 */
class ActionRoutes(lobbyStatePort: LobbyStatePort, gameEnginePort: InboundPort)(using
    ec: ExecutionContext
):

  private def handleAction(
      secret: String,
      lobbyId: LobbyId,
      actionBuilder: PlayerId => GameAction
  ): Future[Either[LobbyError, Unit]] =
    lobbyStatePort
      .getAuthLobby(lobbyId, secret)
      .flatMap:
        case Right((player, lobby)) =>
          if lobby.status != LobbyStatus.IN_GAME then Future.successful(Left(LobbyError.GamePaused))
          else
            gameEnginePort
              .submitAction(lobbyId, actionBuilder(player.id))
              .flatMap:
                case Left(gameError) =>
                  Future.successful(Left(LobbyError.GameActionRejected(gameError)))
                case Right(_) =>
                  lobbyStatePort.clearPlayerStrikes(lobbyId, player.id).map(_ => Right(()))
        case Left(err) =>
          Future.successful(Left(err))

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
