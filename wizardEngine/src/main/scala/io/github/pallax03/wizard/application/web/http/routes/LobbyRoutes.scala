package io.github.pallax03.wizard.application.web.http.routes

import scala.concurrent.{ExecutionContext, Future}

import io.github.pallax03.wizard.application.web.http.*
import io.github.pallax03.wizard.application.web.http.endpoints.*
import io.github.pallax03.wizard.engine.lobby.*
import io.github.pallax03.wizard.engine.model.events.SystemEvent
import io.github.pallax03.wizard.engine.ports.{InboundPort, LobbyStatePort}

import sttp.tapir.server.ServerEndpoint

/** HTTP routes for the Lobby domain. */
class LobbyRoutes(
    lobbyStatePort: LobbyStatePort,
    gameEngine: InboundPort
)(using
    ec: ExecutionContext
):

  import cats.data.EitherT
  import cats.implicits.*

  private def addPlayerToLobby(
      lobbyId: LobbyId,
      req: JoinLobbyRequest
  ): Future[Either[LobbyError, AuthLobbyPlayer]] =
    val actualSecret = req.difficulty match
      case Some(_) => None
      case None    => Some(req.secret.getOrElse(java.util.UUID.randomUUID().toString))

    lobbyStatePort
      .addPlayer(lobbyId, req.name, req.difficulty, actualSecret)
      .map:
        case Right(player) => Right(AuthLobbyPlayer(lobbyId, player.id, player.secret))
        case Left(error)   => Left(error)

  private val createLobbyEndpoint: ServerEndpoint[Any, Future] =
    LobbyEndpoints.createLobby.serverLogic { req => addPlayerToLobby(LobbyId.generate, req) }

  private val joinLobbyEndpoint: ServerEndpoint[Any, Future] =
    LobbyEndpoints.joinLobby.serverLogic { (lobbyId, req) => addPlayerToLobby(lobbyId, req) }

  private val getLobbyInfoEndpoint: ServerEndpoint[Any, Future] =
    LobbyEndpoints.getLobbyInfo.serverLogic { lobbyId =>
      EitherT(lobbyStatePort.getLobby(lobbyId)).map { lobby =>
        LobbyStateResponse(
          lobbyId,
          lobby.status,
          lobby.players.map(p =>
            PublicPlayerInfo(p.id, p.name, p.difficulty, p.isOnline, p.strikes)
          ),
          lobby.configuration
        )
      }.value
    }

  private val getPlayerGameEndpoint: ServerEndpoint[Any, Future] =
    LobbyEndpoints.getPlayerGame
      .serverSecurityLogicSuccess(Future.successful)
      .serverLogic { secret => lobbyId =>
        (for
          (player, lobby) <- EitherT(lobbyStatePort.getAuthLobby(lobbyId, secret))
          _ <- EitherT.cond[Future](
            lobby.status != LobbyStatus.WAITING,
            (),
            LobbyError.GameNotFound
          )
          state <- EitherT(gameEngine.getState(lobbyId, player.id).map(Right(_)).recoverWith {
            case _ =>
              lobbyStatePort
                .updateLobby[Unit](lobbyId) { l =>
                  Right(((), l.copy(status = LobbyStatus.WAITING), None))
                }
                .map(_ => Left(LobbyError.GameNotFound))
          })
        yield state).value
      }

  private val startGameEndpoint: ServerEndpoint[Any, Future] =
    LobbyEndpoints.startGame
      .serverSecurityLogicSuccess(Future.successful)
      .serverLogic { secret => lobbyId =>
        EitherT(lobbyStatePort.updateAuthLobby[Lobby](lobbyId, secret) { (player, lobby) =>
          for _ <- lobby.validateStartOrResume
          yield (
            lobby,
            lobby.copy(status = LobbyStatus.IN_GAME),
            if lobby.status == LobbyStatus.PAUSED then Option(SystemEvent.resumed(player.id))
            else Option.empty
          )
        }).flatMap { oldLobby =>
          EitherT.right[LobbyError](
            if oldLobby.status == LobbyStatus.WAITING then
              gameEngine.startGame(lobbyId, oldLobby.players.map(_.id), oldLobby.configuration)
            else gameEngine.resumeGame(lobbyId)
          )
        }.value
      }

  private val pauseGameEndpoint: ServerEndpoint[Any, Future] =
    LobbyEndpoints.pauseGame
      .serverSecurityLogicSuccess(Future.successful)
      .serverLogic { secret => lobbyId =>
        EitherT(lobbyStatePort.updateAuthLobby[Unit](lobbyId, secret) { (player, lobby) =>
          if lobby.status == LobbyStatus.IN_GAME || lobby.status == LobbyStatus.DISCONNECTING then
            Right(
              ((), lobby.copy(status = LobbyStatus.PAUSED), Some(SystemEvent.paused(player.id)))
            )
          else Left(LobbyError.GameNotFound)
        }).value
      }

  private val updateConfigurationEndpoint: ServerEndpoint[Any, Future] =
    LobbyEndpoints.updateConfiguration
      .serverSecurityLogicSuccess(Future.successful)
      .serverLogic { secret => input =>
        val (lobbyId, config) = input
        EitherT(lobbyStatePort.updateAuthLobby[GameConfiguration](lobbyId, secret) {
          (player, lobby) =>
            config.validate match
              case Left(err) => Left(LobbyError.ConfigurationInvalid(err))
              case Right(_) =>
                Right(
                  (
                    config,
                    lobby.copy(configuration = config),
                    Some(SystemEvent.configUpdated(player.id))
                  )
                )
        }).value
      }

  private val removePlayerEndpoint: ServerEndpoint[Any, Future] =
    LobbyEndpoints.removePlayer
      .serverSecurityLogicSuccess(Future.successful)
      .serverLogic { secret => (lobbyId, playerId) =>
        EitherT(lobbyStatePort.updateAuthLobby[Unit](lobbyId, secret) { (_, lobby) =>
          if lobby.status == LobbyStatus.WAITING then
            for newLobby <- lobby.removePlayer(playerId)
            yield ((), newLobby, Some(SystemEvent.left(playerId)))
          else Left(LobbyError.GameInProgress)
        }).value
      }

  private val returnLobbyEndpoint: ServerEndpoint[Any, Future] =
    LobbyEndpoints.returnLobby
      .serverSecurityLogicSuccess(Future.successful)
      .serverLogic { secret => lobbyId =>
        EitherT(lobbyStatePort.updateAuthLobby[Unit](lobbyId, secret) { (_, lobby) =>
          if lobby.status == LobbyStatus.FINISHED || lobby.status == LobbyStatus.PAUSED then
            Right(((), lobby.copy(status = LobbyStatus.WAITING), None))
          else Left(LobbyError.GameInProgress)
        }).flatMap { _ =>
          EitherT.right[LobbyError](gameEngine.deleteGame(lobbyId))
        }.value
      }

  val all: List[ServerEndpoint[Any, Future]] = List(
    createLobbyEndpoint,
    joinLobbyEndpoint,
    getLobbyInfoEndpoint,
    startGameEndpoint,
    pauseGameEndpoint,
    returnLobbyEndpoint,
    updateConfigurationEndpoint,
    removePlayerEndpoint,
    getPlayerGameEndpoint
  )
