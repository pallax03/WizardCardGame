package it.unibo.pps.wizard.engine.lobby

import java.util.UUID

/** Represents the status of a Lobby. */
enum LobbyStatus:
  case WAITING, IN_GAME, FINISHED

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
    players: List[LobbyPlayer],
    status: LobbyStatus
)
