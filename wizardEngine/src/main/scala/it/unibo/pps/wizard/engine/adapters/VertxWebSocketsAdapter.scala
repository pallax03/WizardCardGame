package it.unibo.pps.wizard.engine.adapters

import io.vertx.core.http.ServerWebSocket
import it.unibo.pps.wizard.engine.lobby.LobbyId
import it.unibo.pps.wizard.engine.model.basic.PlayerId
import it.unibo.pps.wizard.engine.ports.{RedisPubSubPort, WebSocketsPort}

import scala.concurrent.Future

class VertxWebSocketsAdapter(
    private val redisPubSubPort: RedisPubSubPort
) extends WebSocketsPort:

  val sockets: Map[LobbyId, Map[PlayerId, ServerWebSocket]] = Map.empty

  /** @inheritdoc */
  override def subscribeToLobbyEvents(lobbyId: LobbyId, playerId: PlayerId, ws: ServerWebSocket): Future[Unit] =
    val _ = redisPubSubPort
    ???

  /** @inheritdoc */
  override def close: Future[Unit] =
    ???
