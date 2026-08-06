package it.unibo.pps.wizard.engine.ports

import scala.concurrent.Future

/**
 * Internal port to manage the real-time event synchronization across distributed instances.
 * 
 * This port relies on a Publish/Subscribe pattern (e.g., Redis Pub/Sub) rather than a static 
 * Key-Value store, enabling reactive event-driven architectures.
 * 
 * Flow for Game Engine Outbound Adapter:
 * - Whenever the core game engine emits a new GameState or Event, the outbound adapter 
 *   calls `publish` to broadcast the state to all connected nodes.
 * 
 * Flow for WebSockets Verticle / Adapter:
 * - When a client successfully connects (after validation via LobbyStatePort), 
 *   the adapter calls `subscribe` on the lobby's specific channel.
 * - Every time a message arrives from the channel, the provided `onMessage` callback is fired, 
 *   which will route the JSON payload directly to the client's WebSocket.
 * - On client disconnect, if it's the last client for that lobby on the current node, 
 *   `unsubscribe` should be called to free resources.
 */
trait RedisPubSubPort:

  /**
   * Publishes a message to a specific channel.
   *
   * @param channel     the channel name (e.g., "channel:lobby:UUID").
   * @param jsonMessage the serialized event or state to broadcast.
   * @return a Future completing when the message is successfully published.
   */
  def publish(channel: String, jsonMessage: String): Future[Unit]

  /**
   * Subscribes to a specific channel to receive real-time messages.
   *
   * @param channel   the channel name to listen to.
   * @param onMessage the callback invoked whenever a new message is received on this channel.
   * @return a Future completing when the subscription is successfully established.
   */
  def subscribe(channel: String, onMessage: String => Unit): Future[Unit]

  /**
   * Removes the subscription from a specific channel.
   *
   * @param channel the channel name to unsubscribe from.
   * @return a Future completing when the subscription is removed.
   */
  def unsubscribe(channel: String): Future[Unit]