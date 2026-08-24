package it.unibo.pps.wizard.engine.ports

import it.unibo.pps.wizard.engine.lobby.LobbyId
import it.unibo.pps.wizard.engine.model.basic.PlayerId
import it.unibo.pps.wizard.util.ChannelsKeys

import scala.concurrent.Future

trait Subscription:
  /** Cancels this specific subscription. */
  def cancel(): Future[Unit]

trait PubSubPort:

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
   * @param onMessage the callback invoked whenever a new message is received.
   * @return a Future completing with a Subscription to cancel it later.
   */
  def subscribe(channel: String, onMessage: String => Unit): Future[Subscription]

  /** Subscribes to the global lobby channel. */
  def subscribeToLobby(lobbyId: LobbyId, onMessage: String => Unit): Future[Subscription] =
    subscribe(ChannelsKeys.pubSubLobbyChannel(lobbyId), onMessage)

  /** Subscribes to a specific player's channel within a lobby. */
  def subscribeToPlayer(
      lobbyId: LobbyId,
      playerId: PlayerId,
      onMessage: String => Unit
  ): Future[Subscription] =
    subscribe(ChannelsKeys.pubSubLobbyPlayerChannel(lobbyId, playerId), onMessage)

  /** Publishes a system message indicating a player joined the lobby. */
  def publishPlayerJoined(lobbyId: LobbyId, playerId: PlayerId): Future[Unit] =
    publish(ChannelsKeys.pubSubLobbyChannel(lobbyId), s"""{"type":"system","playerId":${playerId.toInt},"action":"joined"}""")

  /** Publishes a system message indicating a player left the lobby. */
  def publishPlayerLeft(lobbyId: LobbyId, playerId: PlayerId): Future[Unit] =
    publish(ChannelsKeys.pubSubLobbyChannel(lobbyId), s"""{"type":"system","playerId":${playerId.toInt},"action":"left"}""")
