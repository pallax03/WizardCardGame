package io.github.pallax03.wizard.engine.ports

import scala.concurrent.{ExecutionContext, Future}

import io.github.pallax03.wizard.engine.lobby.LobbyId
import io.github.pallax03.wizard.engine.model.basic.PlayerId
import io.github.pallax03.wizard.util.ChannelsKeys

/** Handle representing an active topic subscription that can be canceled. */
trait Subscription:
  def cancel(): Future[Unit]

/**
 * Distributed messaging port providing publish/subscribe capabilities across cluster nodes.
 *
 * Implemented via message brokers (such as Redis Pub/Sub) to route game events to
 * interested WebSocket sessions, regardless of which node hosts the client connection.
 */
trait PubSubPort:

  /** Publishes a serialized JSON payload to the specified broker channel. */
  def publish(channel: String, jsonMessage: String): Future[Unit]

  /** Subscribes an event callback to a broker channel, returning a cancellable [[Subscription]]. */
  def subscribe(channel: String, onMessage: String => Unit): Future[Subscription]

  /**
   * Establishes a composite subscription covering both lobby broadcast events
   * and private unicast player events.
   *
   * Canceling the returned [[Subscription]] unsubscribes from both channels simultaneously.
   */
  def subscribePlayer(
      lobbyId: LobbyId,
      playerId: PlayerId,
      onMessage: String => Unit
  )(using ExecutionContext): Future[Subscription] =
    for
      lobbySub <- subscribe(ChannelsKeys.pubSubLobbyChannel(lobbyId), onMessage)
      playerSub <- subscribe(ChannelsKeys.pubSubLobbyPlayerChannel(lobbyId, playerId), onMessage)
    yield new Subscription:
      override def cancel(): Future[Unit] =
        for
          _ <- lobbySub.cancel()
          _ <- playerSub.cancel()
        yield ()
