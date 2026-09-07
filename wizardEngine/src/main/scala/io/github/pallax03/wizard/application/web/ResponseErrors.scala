package io.github.pallax03.wizard.application.web

import io.github.pallax03.wizard.engine.lobby.LobbyId

sealed trait ResponseErrors:
  def message: String
  def code: String
  def statusCode: Int

object ResponseErrors:

  sealed trait NotFoundError extends ResponseErrors:
    override def statusCode: Int = 404

  sealed trait BadRequestError extends ResponseErrors:
    override def statusCode: Int = 400

  sealed trait InternalError extends ResponseErrors:
    override def statusCode: Int = 500

  sealed trait UnauthorizedError extends ResponseErrors:
    override def statusCode: Int = 401

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

  case class CustomBadRequest(message: String, code: String) extends BadRequestError
  
  case class UnknownResponseError(message: String, code: String) extends ResponseErrors:
    override def statusCode: Int = 500