package it.unibo.pps.wizard.engine.adapters

import it.unibo.pps.wizard.engine.ports.GameEngineInboundPort
import it.unibo.pps.wizard.engine.ports.RedisPubSubPort
import it.unibo.pps.wizard.engine.ports.WebSocketsPort

import scala.concurrent.Future

/**
 * Adapter that implements the WebSocketsPort.
 *
 * It acts as a bridge between the Vert.x WebSocket Verticle and the internal engine ports.
 * - Forwards incoming JSON strings to the `GameEngineInboundPort` after deserialization.
 * - Binds the WebSocket output stream to the `RedisPubSubPort` subscriptions.
 */
class VertxWebSocketsAdapter(
    private val inboundPort: GameEngineInboundPort,
    private val redisPubSubPort: RedisPubSubPort
) extends WebSocketsPort:

  /** @inheritdoc */
  override def handleClientMessage(jsonMessage: String): Future[Unit] =
    val _ = inboundPort // Suppress unused warning during scaffolding
    // 1. Decode jsonMessage into a GameAction using Circe Codecs
    // 2. If decoding succeeds, call `inboundPort.submitAction(action)`
    // 3. If decoding fails, return a Future.failed with an error
    ???

  /** @inheritdoc */
  override def subscribeLobbyEvents(lobbyId: String, onJsonMessage: String => Unit): Future[Unit] =
    val _ = redisPubSubPort // Suppress unused warning during scaffolding
    // 1. Determine the channel name (e.g., s"channel:lobby:$lobbyId")
    // 2. Call `redisPubSubPort.subscribe(channel, onJsonMessage)`
    // 3. Return the resulting Future so the Verticle knows the subscription is active
    ???
