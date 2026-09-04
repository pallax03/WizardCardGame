package io.github.pallax03.wizard.engine.ports

import scala.concurrent.Future

import io.github.pallax03.wizard.engine.lobby.*
import io.github.pallax03.wizard.engine.model.basic.PlayerId

/**
 * Internal port to manage the persistent state of a Lobby before the game starts.
 *
 * This port is designed to interact with a Key-Value store (e.g., Redis Key-Value).
 * It acts as the single source of truth for the lobby composition across the distributed system.
 *
 * Flow for HTTP API (Verticle):
 * 1. An HTTP request arrives to create a lobby or join an existing one.
 * 2. The HTTP Verticle uses this port (`saveLobby`) to store or update the lobby state.
 *
 * Flow for WebSockets (Verticle):
 * 1. A client attempts to open a WebSocket connection for a specific lobbyId and playerId.
 * 2. The WebSocket Verticle uses this port (`getLobby`) to fetch the current state.
 * 3. It validates if the playerId is actually part of the lobby.
 * 4. If valid, the connection is accepted; otherwise, it is rejected.
 */
trait LobbyStatePort:

  /**
   * Saves or updates the current state of the lobby.
   *
   * @param lobby the strongly-typed Lobby object to be saved.
   * @return a Future completing when the operation is successfully stored.
   */
  def saveLobby(lobby: Lobby): Future[Unit]

  /**
   * Retrieves the current state of the lobby, if it exists.
   *
   * @param lobbyId the UUID of the lobby.
   * @return a Future containing the Lobby object if found, or None if the lobby does not exist.
   */
  def getLobby(lobbyId: LobbyId): Future[Option[Lobby]]

  /**
   * Retrieves the current state of the lobby, if it exists.
   *
   *   R* @return a Future containing a list of Lobbies, [[List.empty]] if no lobby found.
   */
  def getAllLobbies: Future[List[Lobby]]

  /**
   * Atomically adds a player to the lobby, returning the assigned Player if successful.
   * Fails (returns None) if the lobby is full (max 6 players).
   *
   * @param lobbyId the UUID of the lobby if retrieve fail automatically create a new lobby.
   * @param name the player's name
   * @param bot the bot difficulty, if any
   * @return a Future containing the assigned Player, or None if the lobby is full.
   */
  def addPlayer(
      lobbyId: LobbyId,
      name: String,
      bot: Option[BotsDifficulty] = None
  ): Future[Either[LobbyError, Player]]

  /**
   * Atomically removes a player from the lobby by ID.
   *
   * @param lobbyId the UUID of the lobby.
   * @param playerId the ID of the player to remove.
   * @return a Future containing true if the player was removed, false otherwise.
   */
  def removePlayer(lobbyId: LobbyId, playerId: PlayerId): Future[Boolean]

  /**
   * Attempts to acquire the distributed lock for managing bots in this lobby.
   *
   * @param lobbyId the UUID of the lobby.
   * @param podId unique identifier for the current pod/container.
   * @param ttlSeconds lock expiration time to prevent deadlocks if the pod crashes.
   * @return a Future containing true if the lock was acquired, false otherwise.
   */
  def tryAcquireBotLock(lobbyId: LobbyId, podId: String, ttlSeconds: Long = 30): Future[Boolean]

  /**
   * Updates the online status of a specific player in the lobby.
   *
   * @param lobbyId the UUID of the lobby.
   * @param playerId the ID of the player.
   * @param isOnline the new online status.
   * @return a Future containing true if the player was found and updated, false otherwise.
   */
  def setPlayerOnlineStatus(
      lobbyId: LobbyId,
      playerId: PlayerId,
      isOnline: Boolean
  ): Future[Boolean]
