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

  case class LobbyNotFound(lobbyId: LobbyId) extends NotFoundError:
    val message: String = s"Lobby $lobbyId not found"
    val code = "LOBBY_NOT_FOUND"

  case class GameNotFound(lobbyId: LobbyId) extends NotFoundError:
    val message: String = s"Game not found for lobby: $lobbyId"
    val code = "GAME_NOT_FOUND"

  case object LobbyFull extends BadRequestError:
    val message = "Lobby is full"
    val code = "LOBBY_FULL"

  case object NotEnoughPlayer extends BadRequestError:
    val message = "Need at least 3 player to start a game"
    val code = "NOT_ENOUGH_PLAYER"

  case object GameInProgress extends BadRequestError:
    val message = "Game already started or finished"
    val code = "GAME_ALREADY_STARTED"

  case object PlayersOffline extends BadRequestError:
    val message = "Not all players are online"
    val code = "PLAYERS_OFFLINE"

  case object PlayerNotFound extends NotFoundError:
    val message = "Player not found"
    val code = "PLAYER_NOT_FOUND"

  case object NotAuthenticated extends UnauthorizedError:
    val message = "Not authenticated or invalid secret"
    val code = "NOT_AUTHENTICATED"

  case class GameError(gameError: String) extends BadRequestError:
    val message: String = gameError
    val code = "GAME_ERROR"

  case class InternalServerError(exMsg: String) extends InternalError:
    val message: String = s"Internal error: $exMsg"
    val code = "INTERNAL_ERROR"

  case class UnknownAppError(message: String, code: String) extends AppError
