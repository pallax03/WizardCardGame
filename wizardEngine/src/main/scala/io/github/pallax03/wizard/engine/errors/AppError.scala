package io.github.pallax03.wizard.engine.errors

import io.github.pallax03.wizard.engine.lobby.LobbyId

sealed trait AppError:
  def message: String
  def code: String

object AppError:

  sealed trait NotFoundError extends AppError
  sealed trait BadRequestError extends AppError
  sealed trait InternalError extends AppError
  sealed trait UnauthorizedError extends AppError

  // Lobby Errors
  case class LobbyNotFound(lobbyId: LobbyId) extends NotFoundError:
    val message: String = s"Lobby $lobbyId not found"
    val code = "LOBBY_NOT_FOUND"

  case object LobbyFull extends BadRequestError:
    val message = "Lobby is full"
    val code = "LOBBY_FULL"

  case object GameInProgress extends BadRequestError:
    val message = "Game already started or finished"
    val code = "GAME_ALREADY_STARTED"

  case object PlayersOffline extends BadRequestError:
    val message = "Not all players are online"
    val code = "PLAYERS_OFFLINE"

  case object PlayerOrLobbyNotFound extends NotFoundError:
    val message = "Player or lobby not found"
    val code = "NOT_FOUND"

  case object NotAuthenticated extends UnauthorizedError:
    val message = "Not authenticated or invalid secret"
    val code = "NOT_AUTHENTICATED"

  // Action / Game Errors
  case class GameError(gameError: String) extends BadRequestError:
    val message: String = gameError
    val code = "GAME_ERROR"

  // Internal Errors
  case class InternalServerError(exMsg: String) extends InternalError:
    val message: String = s"Internal error: $exMsg"
    val code = "INTERNAL_ERROR"

  case class UnknownAppError(message: String, code: String) extends AppError
