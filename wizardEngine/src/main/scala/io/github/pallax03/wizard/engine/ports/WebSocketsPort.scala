package io.github.pallax03.wizard.engine.ports

import scala.concurrent.Future

import io.vertx.core.http.ServerWebSocket

import io.github.pallax03.wizard.engine.lobby.LobbyId
import io.github.pallax03.wizard.engine.model.basic.PlayerId

/**
 * Outbound delivery port bridging distributed broker events to connected WebSocket clients.
 *
 * Manages WebSocket event streaming for connected players, forwarding real-time lobby updates
 * and private player notifications directly over Vert.x [[ServerWebSocket]] sessions.
 */
trait WebSocketsPort:

  /**
   * Binds the client's WebSocket connection to the real-time event stream for the specified lobby.
   *
   * Pipes both broadcast and player-scoped unicast events from [[PubSubPort]] to the socket.
   */
  def subscribeToLobbyEvents(
      lobbyId: LobbyId,
      playerId: PlayerId,
      ws: ServerWebSocket
  ): Future[Unit]

  /** Terminates the active WebSocket connection for a player and cancels associated subscriptions. */
  def close(lobbyId: LobbyId, playerId: PlayerId): Future[Unit]
