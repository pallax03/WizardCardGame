package it.unibo.pps.wizard.engine.ports

import it.unibo.pps.wizard.engine.lobby.LobbyId
import it.unibo.pps.wizard.engine.model.basic.PlayerId
import it.unibo.pps.wizard.util.ChannelsKeys

import scala.concurrent.{Future, ExecutionContext}

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

  /**
   * Subscribes a player to both the global lobby channel and their specific player channel,
   * and publishes a system message indicating they joined.
   * Returns a single Subscription that, when closed, unsubscribes from both channels
   * and publishes a system message indicating the player left.
   */
  def subscribePlayer(
      lobbyId: LobbyId,
      playerId: PlayerId,
      onMessage: String => Unit
  )(using scala.concurrent.ExecutionContext): Future[Subscription] =
    for
      lobbySub <- subscribe(ChannelsKeys.pubSubLobbyChannel(lobbyId), onMessage)
      playerSub <- subscribe(ChannelsKeys.pubSubLobbyPlayerChannel(lobbyId, playerId), onMessage)
      _ <- publish(ChannelsKeys.pubSubLobbyChannel(lobbyId), s"""{"type":"system","playerId":${playerId.toInt},"action":"joined"}""")
    yield new Subscription:
      override def cancel(): Future[Unit] =
        for
          _ <- publish(ChannelsKeys.pubSubLobbyChannel(lobbyId), s"""{"type":"system","playerId":${playerId.toInt},"action":"left"}""")
          _ <- lobbySub.cancel()
          _ <- playerSub.cancel()
        yield ()
