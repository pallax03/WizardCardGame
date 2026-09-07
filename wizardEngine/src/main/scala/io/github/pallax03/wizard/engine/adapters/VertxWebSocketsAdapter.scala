package io.github.pallax03.wizard.engine.adapters

import scala.collection.concurrent.TrieMap
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Try

import io.vertx.core.http.ServerWebSocket
import io.vertx.core.json.JsonObject

import io.github.pallax03.wizard.codecs.engine.model.SystemEventCodecs.given
import io.github.pallax03.wizard.codecs.syntax.CodecSyntax.*
import io.github.pallax03.wizard.engine.lobby.LobbyId
import io.github.pallax03.wizard.engine.model.basic.PlayerId
import io.github.pallax03.wizard.engine.model.events.SystemEvent
import io.github.pallax03.wizard.engine.ports.{
  LobbyStatePort,
  PubSubPort,
  Subscription,
  WebSocketsPort
}
import io.github.pallax03.wizard.util.ChannelsKeys

case class ClientSession(ws: ServerWebSocket, sub: Subscription)

class VertxWebSocketsAdapter(
    val pubSubPort: PubSubPort,
    val lobbyStatePort: LobbyStatePort
) extends WebSocketsPort:

  private val sessions: TrieMap[(LobbyId, PlayerId), ClientSession] = TrieMap.empty

  /** @inheritdoc */
  override def subscribeToLobbyEvents(
      lobbyId: LobbyId,
      playerId: PlayerId,
      ws: ServerWebSocket
  ): Future[Unit] =
    ws.closeHandler: _ =>
      this.close(lobbyId, playerId, ws)
    ws.exceptionHandler: _ =>
      this.close(lobbyId, playerId, ws)

    // Forwarding Messages
    ws.textMessageHandler: text =>
      Try:
        val json = new JsonObject(text)
        if json.containsKey("destinationId") then
          val destId = PlayerId(json.getInteger("destinationId"))
          pubSubPort.publish(ChannelsKeys.pubSubLobbyPlayerChannel(lobbyId, destId), text)
        else pubSubPort.publish(ChannelsKeys.pubSubLobbyChannel(lobbyId), text)

    pubSubPort
      .subscribePlayer(lobbyId, playerId, rawJson => ws.writeTextMessage(rawJson))
      .map: sub =>
        lobbyStatePort.setPlayerOnlineStatus(lobbyId, playerId, true)
        val msg = SystemEvent.online(playerId).toJson
        pubSubPort.publish(ChannelsKeys.pubSubLobbyChannel(lobbyId), msg)
        sessions.put((lobbyId, playerId), ClientSession(ws, sub))

  /** @inheritdoc */
  override def close(lobbyId: LobbyId, playerId: PlayerId, ws: ServerWebSocket): Future[Unit] =
    sessions.get((lobbyId, playerId)) match
      // A stale close must not drop a newer session nor mark its player offline.
      case Some(session) if session.ws eq ws =>
        sessions.remove((lobbyId, playerId))
        Try(session.ws.close())
        lobbyStatePort.setPlayerOnlineStatus(lobbyId, playerId, false)
        val msg = SystemEvent.offline(playerId).toJson
        pubSubPort.publish(ChannelsKeys.pubSubLobbyChannel(lobbyId), msg)
        session.sub.cancel()
      case _ =>
        Future.unit
