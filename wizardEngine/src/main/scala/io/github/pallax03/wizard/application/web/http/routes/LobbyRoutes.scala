package io.github.pallax03.wizard.application.web.http.routes

import scala.concurrent.{ExecutionContext, Future}

import io.github.pallax03.wizard.application.web.http.ErrorResponse
import io.github.pallax03.wizard.application.web.http.endpoints.*
import io.github.pallax03.wizard.engine.configuration.GameConfiguration
import io.github.pallax03.wizard.engine.lobby.*
import io.github.pallax03.wizard.engine.ports.{InboundPort, LobbyStatePort}

import sttp.tapir.server.ServerEndpoint

/**
 * HTTP routes for the Lobby domain.
 *
 * Standard layout (shared across all `*Routes`):
 *   1. private helpers that encapsulate business logic and return `Either[ErrorResponse, Output]`
 *   2. `ServerEndpoint` vals wiring each `LobbyEndpoints.*` via `serverLogic`
 *   3. `val all` aggregating the endpoints for `HttpServerVerticle`
 */
class LobbyRoutes(lobbyStatePort: LobbyStatePort, gameEngine: InboundPort)(using
    ec: ExecutionContext
):

  private def addPlayerToLobby(
      lobbyId: LobbyId,
      req: JoinLobbyRequest
  ): Future[Either[ErrorResponse, LobbyPlayerResponse]] =
    lobbyStatePort
      .addPlayer(lobbyId, req.name, req.bot)
      .map:
        case Some(player) => Right(LobbyPlayerResponse(lobbyId, player.id))
        case None         => Left(ErrorResponse("Lobby is full", "LOBBY_FULL"))

  val createLobbyEndpoint: ServerEndpoint[Any, Future] =
    LobbyEndpoints.createLobby.serverLogic { req =>
      val lobbyId: LobbyId = LobbyId.generate
      addPlayerToLobby(lobbyId, req)
    }

  val joinLobbyEndpoint: ServerEndpoint[Any, Future] =
    LobbyEndpoints.joinLobby.serverLogic { case (lobbyId, req) =>
      addPlayerToLobby(lobbyId, req)
    }

  val getLobbyInfoEndpoint: ServerEndpoint[Any, Future] =
    LobbyEndpoints.getLobbyInfo.serverLogic { lobbyId =>
      lobbyStatePort
        .getLobby(lobbyId)
        .map:
          case Some(lobby) => Right(LobbyStateResponse(lobbyId, lobby.players))
          case None        => Left(ErrorResponse(s"Lobby $lobbyId not found", "LOBBY_NOT_FOUND"))
    }

  val startGameEndpoint: ServerEndpoint[Any, Future] =
    LobbyEndpoints.startGame.serverLogic { lobbyId =>
      lobbyStatePort
        .getLobby(lobbyId)
        .flatMap:
          case Some(lobby) =>
            if lobby.status == LobbyStatus.WAITING then
              if lobby.players.forall(_.isOnline) then
                lobbyStatePort
                  .saveLobby(lobby.copy(status = LobbyStatus.IN_GAME))
                  .flatMap(_ =>
                    gameEngine.startGame(
                      lobbyId,
                      lobby.players.map(_.id),
                      GameConfiguration(1000, lobby.players)
                    )
                  )
                  .map(_ => Right(GameStartedResponse("Game started")))
              else
                Future.successful(
                  Left(ErrorResponse("Not all players are online", "PLAYERS_OFFLINE"))
                )
            else
              Future.successful(
                Left(ErrorResponse("Game already started or finished", "GAME_ALREADY_STARTED"))
              )
          case None =>
            Future.successful(
              Left(ErrorResponse(s"Lobby $lobbyId not found", "LOBBY_NOT_FOUND"))
            )
    }

  val removePlayerEndpoint: ServerEndpoint[Any, Future] =
    LobbyEndpoints.removePlayer.serverLogic { req =>
      lobbyStatePort
        .removePlayer(req.lobbyId, req.playerId)
        .map: success =>
          if success then Right(())
          else Left(ErrorResponse("Player or lobby not found", "NOT_FOUND"))
    }

  val all: List[ServerEndpoint[Any, Future]] = List(
    createLobbyEndpoint,
    joinLobbyEndpoint,
    getLobbyInfoEndpoint,
    startGameEndpoint,
    removePlayerEndpoint
  )
