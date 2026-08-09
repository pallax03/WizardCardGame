package it.unibo.pps.wizard.engine.ports

import it.unibo.pps.wizard.engine.lobby.Lobby

import scala.concurrent.Future

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
  def getLobby(lobbyId: String): Future[Option[Lobby]]
