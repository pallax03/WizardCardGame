package io.github.pallax03.wizard.engine.lobby

import java.util.UUID

import io.github.pallax03.wizard.engine.configuration.GameConfiguration

/** Represents the status of a Lobby. */
enum LobbyStatus:
  case WAITING, IN_GAME, PAUSED, FINISHED

opaque type LobbyId = String

object LobbyId:
  def apply(id: String): LobbyId = id
  def generate: LobbyId = UUID.randomUUID().toString

/**
 * Represents a game lobby in the application layer.
 *
 * @param uuid the globally unique identifier of the lobby.
 * @param players the list of players currently in the lobby.
 * @param status the current lifecycle status of the lobby.
 */
case class Lobby(
    uuid: LobbyId,
    players: List[Player],
    status: LobbyStatus,
    configuration: GameConfiguration
):

  import io.github.pallax03.wizard.application.web.ResponseErrors

  def validateStartOrResume: Either[ResponseErrors, Unit] = status match
    case LobbyStatus.WAITING =>
      if players.size < 3 then Left(ResponseErrors.NotEnoughPlayer)
      else if !players.forall(_.isOnline) then Left(ResponseErrors.PlayersOffline)
      else Right(())
    case LobbyStatus.PAUSED => Right(())
    case _                  => Left(ResponseErrors.GameInProgress)
