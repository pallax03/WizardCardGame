package it.unibo.pps.wizard.engine.ports

import scala.concurrent.Future

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
