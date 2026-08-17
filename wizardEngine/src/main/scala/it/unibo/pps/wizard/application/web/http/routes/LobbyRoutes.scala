package it.unibo.pps.wizard.application.web.http.routes

import it.unibo.pps.wizard.application.web.http.*
import it.unibo.pps.wizard.application.web.http.endpoints.{ErrorResponse, GameStartedResponse, LobbyCreatedResponse, LobbyEndpoints, LobbyStateResponse}
import it.unibo.pps.wizard.engine.configuration.GameConfiguration
import it.unibo.pps.wizard.engine.lobby.*
import it.unibo.pps.wizard.engine.ports.InboundPort
import it.unibo.pps.wizard.engine.ports.LobbyStatePort
import sttp.tapir.server.ServerEndpoint

import scala.concurrent.{ExecutionContext, Future}

class LobbyRoutes(lobbyStatePort: LobbyStatePort, gameEngine: InboundPort)(implicit ec: ExecutionContext):

  val createLobbyServerEndpoint = LobbyEndpoints.createLobby
    .serverLogic: req =>
      val lobbyId = LobbyId.generate
      lobbyStatePort
        .addPlayer(lobbyId, req.name, req.bot)
        .map:
          case Some(player) =>
            Right(LobbyCreatedResponse(lobbyId, player.id))
          case None =>
            Left(ErrorResponse("Lobby is full", "LOBBY_FULL"))

  val joinLobbyServerEndpoint = LobbyEndpoints.joinLobby
    .serverLogic:
      case (rawLobbyId, req) =>
        val lobbyId = LobbyId(rawLobbyId)
        lobbyStatePort
          .addPlayer(lobbyId, req.name, req.bot)
          .map:
            case Some(player) =>
              Right(LobbyStateResponse(lobbyId, List(player)))
            case None =>
              Left(ErrorResponse("Lobby is full", "LOBBY_FULL"))

  val getLobbyInfoServerEndpoint = LobbyEndpoints.getLobbyInfo
    .serverLogic: rawLobbyId =>
      val lobbyId = LobbyId(rawLobbyId)
      lobbyStatePort
        .getLobby(lobbyId)
        .map:
          case Some(lobby) =>
            Right(LobbyStateResponse(lobbyId, lobby.players))
          case None =>
            Left(ErrorResponse(s"Lobby $rawLobbyId not found", "LOBBY_NOT_FOUND"))

  val startGameServerEndpoint = LobbyEndpoints.startGame
    .serverLogic: rawLobbyId =>
      val lobbyId = LobbyId(rawLobbyId)
      lobbyStatePort
        .getLobby(lobbyId)
        .flatMap:
          case Some(lobby) =>
            if lobby.status == LobbyStatus.WAITING then
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
                Left(ErrorResponse("Game already started or finished", "GAME_ALREADY_STARTED"))
              )
          case None =>
            Future.successful(Left(ErrorResponse(s"Lobby $rawLobbyId not found", "LOBBY_NOT_FOUND")))

  val all: List[ServerEndpoint[Any, Future]] = List(
    createLobbyServerEndpoint,
    joinLobbyServerEndpoint,
    getLobbyInfoServerEndpoint,
    startGameServerEndpoint
  )