package it.unibo.pps.wizard.engine.ports

import scala.concurrent.Future

/**
 * External port representing the boundary between the WebSocket Verticle (Network) and the Game Engine logic.
 *
 * This trait is implemented by the WebSocketsAdapter, which acts as a Facade.
 * It glues together the incoming raw WebSocket messages and the outbound Pub/Sub events.
 *
 * Flow for WebSockets Verticle:
 * 1. Client sends a message -> Verticle intercepts it -> calls `handleClientMessage`.
 * 2. Client opens connection -> Verticle validates it -> calls `subscribeLobbyEvents` 
 *    passing a callback (e.g., `socket.writeTextMessage(...)`) to forward Redis events.
 *
 * todo: maybe this port can be deleted, and implement those functions only in the adapter
 */
trait WebSocketsPort:

  /**
   * Handles an incoming message from a connected WebSocket client.
   * The adapter is responsible for deserializing this message (e.g., into a GameAction)
   * and submitting it to the Game Engine's Inbound Port.
   *
   * @param jsonMessage the serialized action received from the client.
   * @return a Future completing when the action is successfully submitted to the engine.
   */
  def handleClientMessage(jsonMessage: String): Future[Unit]

  /**
   * Connects the WebSocket client to the real-time event stream of a specific lobby.
   * The adapter relies on the `RedisPubSubPort` to achieve this.
   *
   * @param lobbyId       the unique identifier of the lobby.
   * @param onJsonMessage the callback to execute when a new event arrives from the Pub/Sub system. 
   *                      The Verticle should provide a function that writes to the actual socket.
   * @return a Future completing when the subscription is successfully established.
   */
  def subscribeLobbyEvents(lobbyId: String, onJsonMessage: String => Unit): Future[Unit]
