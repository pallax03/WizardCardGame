package it.unibo.pps.wizard.engine.lobby

/** Represents the status of a Lobby. */
enum LobbyStatus:
  case WAITING, IN_GAME, FINISHED

opaque type LobbyId = String

object LobbyId:
  def apply(id: String): LobbyId = id  

/**
 * Represents a game lobby in the application layer.
 * todo: This object is typically serialized and persisted in Redis.
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
