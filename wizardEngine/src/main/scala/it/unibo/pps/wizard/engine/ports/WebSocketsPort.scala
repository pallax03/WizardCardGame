package it.unibo.pps.wizard.engine.ports

import io.vertx.core.http.ServerWebSocket
import it.unibo.pps.wizard.engine.lobby.LobbyId
import it.unibo.pps.wizard.engine.model.basic.PlayerId

import scala.concurrent.Future

trait WebSocketsPort:

  /**
   * Connects the WebSocket client to the real-time event stream of a specific lobby.
   * The adapter relies on the `RedisPubSubPort` to achieve this.
   *
   * @param lobbyId       the unique identifier of the lobby.
   * @param playerId      the unique identifier of the player.
   * @param ws            the WebSocket connection.
   *
   * @return a Future completing when the subscription is successfully established.
   */
  def subscribeToLobbyEvents(
      lobbyId: LobbyId,
      playerId: PlayerId,
      ws: ServerWebSocket
  ): Future[Unit]

  /**
   * Close socket.
   *
   * @return a Future completing when the socket is successfully closed.
   */
  def close: Future[Unit]
