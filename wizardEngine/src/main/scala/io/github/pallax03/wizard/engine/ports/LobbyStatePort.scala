package io.github.pallax03.wizard.engine.ports

import scala.concurrent.Future

import io.github.pallax03.wizard.engine.lobby.*
import io.github.pallax03.wizard.engine.model.basic.PlayerId
import io.github.pallax03.wizard.engine.model.events.SystemEvent

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
   * Retrieves the current state of the lobby.
   *
   * @param lobbyId the UUID of the lobby.
   * @return a Future containing the Lobby object if found, or LobbyError.LobbyNotFound.
   */
  def getLobby(lobbyId: LobbyId): Future[Either[LobbyError, Lobby]]

  /**
   * Retrieves the lobby and authenticates the player using the provided secret.
   *
   * @param lobbyId the UUID of the lobby.
   * @param secret the secret of the player.
   * @return a Future containing the Player and Lobby if successful, or a LobbyError.
   */
  def getAuthLobby(lobbyId: LobbyId, secret: String): Future[Either[LobbyError, (Player, Lobby)]]

  /**
   * Atomically updates an existing lobby state using CAS.
   *
   * @param lobbyId the UUID of the lobby.
   * @param f function to apply the update. It receives the current lobby.
   *          It must return an Either containing a LobbyError, or a tuple:
   *          (Return value of type A, The updated Lobby, An optional SystemEvent to publish).
   */
  def updateLobby[A](lobbyId: LobbyId)(
      f: Lobby => Either[LobbyError, (A, Lobby, Option[SystemEvent])]
  ): Future[Either[LobbyError, A]]

  /** Updates an existing lobby state atomically, after authenticating the player. */
  def updateAuthLobby[A](lobbyId: LobbyId, secret: String)(
      f: (Player, Lobby) => Either[LobbyError, (A, Lobby, Option[SystemEvent])]
  ): Future[Either[LobbyError, A]] =
    updateLobby(lobbyId): lobby =>
      lobby.authenticate(secret).flatMap(player => f(player, lobby))

  /**
   * Atomically adds a player to the lobby, returning the assigned Player if successful.
   * Fails (returns None) if the lobby is full (max 6 players).
   *
   * @param lobbyId the UUID of the lobby if retrieve fail automatically create a new lobby.
   * @param name the player's name
   * @param difficulty the bot difficulty, if any
   * @return a Future containing the assigned Player, or None if the lobby is full.
   */
  def addPlayer(
      lobbyId: LobbyId,
      name: String,
      difficulty: Option[BotsDifficulty] = None,
      secret: Option[String] = None
  ): Future[Either[LobbyError, Player]]

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
