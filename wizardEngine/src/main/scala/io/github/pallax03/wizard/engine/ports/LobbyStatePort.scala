package io.github.pallax03.wizard.engine.ports

import scala.concurrent.Future

import io.github.pallax03.wizard.engine.lobby.*
import io.github.pallax03.wizard.engine.model.basic.PlayerId
import io.github.pallax03.wizard.engine.model.events.SystemEvent

/**
 * Distributed persistence port managing lobby room states across the cluster.
 *
 * Serves as the single source of truth in the distributed key-value store (e.g., Redis)
 * for pre-game player registration, authentication tokens, and connectivity tracking.
 */
trait LobbyStatePort:

  /** Retrieves the current state of the lobby from the distributed store. */
  def getLobby(lobbyId: LobbyId): Future[Either[LobbyError, Lobby]]

  /** Retrieves the lobby state and verifies the player's private session secret. */
  def getAuthLobby(lobbyId: LobbyId, secret: String): Future[Either[LobbyError, (Player, Lobby)]]

  /**
   * Atomically mutates lobby state using optimistic concurrency control or Lua scripting.
   *
   * @param lobbyId target lobby identifier.
   * @param f transformation function returning `(result, newLobby, optionalSystemEvent)`.
   *          If successful, `newLobby` is persisted and the optional [[SystemEvent]] is broadcast.
   */
  def updateLobby[A](lobbyId: LobbyId)(
      f: Lobby => Either[LobbyError, (A, Lobby, Option[SystemEvent])]
  ): Future[Either[LobbyError, A]]

  /** Authenticates the player via session secret prior to executing an atomic lobby mutation. */
  def updateAuthLobby[A](lobbyId: LobbyId, secret: String)(
      f: (Player, Lobby) => Either[LobbyError, (A, Lobby, Option[SystemEvent])]
  ): Future[Either[LobbyError, A]] =
    updateLobby(lobbyId): lobby =>
      lobby.authenticate(secret).flatMap(player => f(player, lobby))

  /** Evicts the specified lobby room from the distributed store. */
  def deleteLobby(lobbyId: LobbyId): Future[Either[LobbyError, Unit]]

  /**
   * Atomically adds a participant to the lobby, initializing the room if it does not exist.
   *
   * Rejects with [[LobbyError.Full]] if the room has reached [[GameConfiguration.MAX_PLAYERS]] (6).
   */
  def addPlayer(
      lobbyId: LobbyId,
      name: String,
      difficulty: Option[BotsDifficulty] = None,
      secret: Option[String] = None
  ): Future[Either[LobbyError, Player]]

  /**
   * Updates player connection status and recalculates the dynamic [[LobbyStatus]].
   *
   * @return future containing the newly evaluated [[LobbyStatus]], or `None` if the player/lobby was not found.
   */
  def setPlayerOnlineStatus(
      lobbyId: LobbyId,
      playerId: PlayerId,
      isOnline: Boolean
  ): Future[Option[LobbyStatus]]

  /** Resets AFK timeout strikes to zero for a player upon activity. */
  def clearPlayerStrikes(lobbyId: LobbyId, playerId: PlayerId): Future[Unit]

  /** Increments the AFK timeout strike counter for a player, returning the new count. */
  def incrementPlayerStrikes(lobbyId: LobbyId, playerId: PlayerId): Future[Int]
