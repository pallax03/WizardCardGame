package io.github.pallax03.wizard.engine.adapters

import scala.collection.concurrent.TrieMap
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Try

import io.vertx.core.buffer.Buffer
import io.vertx.core.Vertx
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

case class ClientSession(ws: ServerWebSocket, sub: Subscription, pingTimerId: Long)

object VertxWebSocketsAdapter:
  private val PING_INTERVAL_MS: Long = 20000
  private val PONG_TIMEOUT_MS: Long = 60000

class VertxWebSocketsAdapter(
    val vertx: Vertx,
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
      this.close(lobbyId, playerId)
    ws.exceptionHandler: _ =>
      this.close(lobbyId, playerId)

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
        
        val pingTimerId = setupHeartbeat(ws)
        sessions.put((lobbyId, playerId), ClientSession(ws, sub, pingTimerId))

  private def setupHeartbeat(ws: ServerWebSocket): Long =
    var lastPong = System.currentTimeMillis()
    ws.pongHandler(_ => lastPong = System.currentTimeMillis())
    
    vertx.setPeriodic(VertxWebSocketsAdapter.PING_INTERVAL_MS, _ => {
      if System.currentTimeMillis() - lastPong > VertxWebSocketsAdapter.PONG_TIMEOUT_MS then
        if !ws.isClosed then ws.close()
      else if !ws.isClosed then
        ws.writePing(Buffer.buffer("ping"))
    })


  /** @inheritdoc */
  override def close(lobbyId: LobbyId, playerId: PlayerId): Future[Unit] =
    sessions.remove((lobbyId, playerId)) match
      case Some(session) =>
        Try(session.ws.close())
        vertx.cancelTimer(session.pingTimerId)
        lobbyStatePort.setPlayerOnlineStatus(lobbyId, playerId, false)
        val msg = SystemEvent.offline(playerId).toJson
        pubSubPort.publish(ChannelsKeys.pubSubLobbyChannel(lobbyId), msg)
        session.sub.cancel()
      case None =>
        Future.unit
