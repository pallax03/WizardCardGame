package io.github.pallax03.wizard.application.web.http.routes

import scala.concurrent.{ExecutionContext, Future}
import io.github.pallax03.wizard.application.web.http.*
import io.github.pallax03.wizard.application.web.http.endpoints.*
import io.github.pallax03.wizard.engine.errors.AppError
import io.github.pallax03.wizard.engine.lobby.*
import io.github.pallax03.wizard.engine.ports.{InboundPort, LobbyStatePort}
import sttp.tapir.server.ServerEndpoint

/**
 * HTTP routes for the Lobby domain.
 */
class LobbyRoutes(
    lobbyStatePort: LobbyStatePort,
    gameEngine: InboundPort
)(using
    ec: ExecutionContext
):

  private def addPlayerToLobby(
      lobbyId: LobbyId,
      req: JoinLobbyRequest
  ): Future[Either[AppError, AuthLobbyPlayer]] =
    val actualSecret = req.bot match
      case Some(_) => None
      case None    => Some(req.secret.getOrElse(java.util.UUID.randomUUID().toString))

    lobbyStatePort
      .addPlayer(lobbyId, req.name, req.bot, actualSecret)
      .map:
        case Right(player) => Right(AuthLobbyPlayer(lobbyId, player.id, player.secret))
        case Left(error)   => Left(error)

  private val createLobbyEndpoint: ServerEndpoint[Any, Future] =
    LobbyEndpoints.createLobby.serverLogic { req =>
      val lobbyId: LobbyId = LobbyId.generate
      addPlayerToLobby(lobbyId, req)
    }

  private val joinLobbyEndpoint: ServerEndpoint[Any, Future] =
    LobbyEndpoints.joinLobby.serverLogic { case (lobbyId, req) =>
      addPlayerToLobby(lobbyId, req)
    }

  private val getLobbyInfoEndpoint: ServerEndpoint[Any, Future] =
    LobbyEndpoints.getLobbyInfo.serverLogic { lobbyId =>
      lobbyStatePort
        .getLobby(lobbyId)
        .map:
          case Some(lobby) =>
            val publicPlayers =
              lobby.players.map(p => PublicPlayerInfo(p.id, p.name, p.difficulty, p.isOnline))
            Right(LobbyStateResponse(lobbyId, lobby.status, publicPlayers))
          case None => Left(AppError.LobbyNotFound(lobbyId))
    }

  private[routes] def withAuth[T](
      secret: String,
      lobbyId: LobbyId
  )(f: (Player, Lobby) => Future[Either[AppError, T]]): Future[Either[AppError, T]] =
    lobbyStatePort
      .getLobby(lobbyId)
      .flatMap:
        case Some(lobby) =>
          lobby.players.find(_.secret.contains(secret)) match
            case Some(player) => f(player, lobby)
            case None         => Future.successful(Left(AppError.NotAuthenticated))
        case None => Future.successful(Left(AppError.LobbyNotFound(lobbyId)))

  private val getPlayerGameEndpoint: ServerEndpoint[Any, Future] =
    LobbyEndpoints.getPlayerGame
      .serverSecurityLogicSuccess(secret => Future.successful(secret))
      .serverLogic { secret => lobbyId =>
        withAuth(secret, lobbyId) { (player, _) =>
          gameEngine
            .getState(lobbyId, player.id)
            .map(state => Right(state))
            .recover { case ex => Left(AppError.InternalServerError(ex.getMessage)) }
        }
      }

  private val startGameEndpoint: ServerEndpoint[Any, Future] =
    LobbyEndpoints.startGame
      .serverSecurityLogicSuccess(secret => Future.successful(secret))
      .serverLogic { secret => lobbyId =>
        withAuth(secret, lobbyId) { (_, lobby) =>
          lobby.status match
            case LobbyStatus.WAITING =>
              if lobby.players.forall(_.isOnline) then
                val updatedLobby = lobby.copy(status = LobbyStatus.IN_GAME)
                lobbyStatePort
                  .saveLobby(updatedLobby)
                  .flatMap(_ =>
                    gameEngine.startGame(
                      lobbyId,
                      lobby.players.map(_.id),
                      lobby.configuration
                    )
                  )
                  .map(_ => Right(GameStartedResponse("Game started")))
              else Future.successful(Left(AppError.PlayersOffline))
            
            case LobbyStatus.PAUSED =>
              lobbyStatePort
                .saveLobby(lobby.copy(status = LobbyStatus.IN_GAME))
                .flatMap: _ =>
                  gameEngine.resumeGame(lobbyId)
                .map(_ => Right(GameStartedResponse("Game resumed")))
            
            case _ => Future.successful(Left(AppError.GameInProgress))
        }
      }

  private val updateConfigurationEndpoint: ServerEndpoint[Any, Future] =
    LobbyEndpoints.updateConfiguration
      .serverSecurityLogicSuccess(secret => Future.successful(secret))
      .serverLogic { secret => input =>
        val (lobbyId, config) = input
        withAuth(secret, lobbyId) { (_, lobby) =>
          val updatedLobby = lobby.copy(configuration = config)
          lobbyStatePort
            .saveLobby(updatedLobby)
            .map(_ => Right(config))
        }
      }

  private val removePlayerEndpoint: ServerEndpoint[Any, Future] =
    LobbyEndpoints.removePlayer
      .serverSecurityLogicSuccess(secret => Future.successful(secret))
      .serverLogic { secret => req =>
        withAuth(secret, req.lobbyId) { (_, _) =>
          lobbyStatePort
            .removePlayer(req.lobbyId, req.playerId)
            .map: success =>
              if success then Right(())
              else Left(AppError.PlayerOrLobbyNotFound)
        }
      }

  val all: List[ServerEndpoint[Any, Future]] = List(
    createLobbyEndpoint,
    joinLobbyEndpoint,
    getLobbyInfoEndpoint,
    startGameEndpoint,
    updateConfigurationEndpoint,
    removePlayerEndpoint,
    getPlayerGameEndpoint
  )
