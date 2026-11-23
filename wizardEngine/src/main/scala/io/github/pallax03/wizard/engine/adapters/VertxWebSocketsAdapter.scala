package io.github.pallax03.wizard.engine.adapters

import scala.collection.concurrent.TrieMap
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Success

import cats.syntax.all.*

import io.vertx.core.Vertx
import io.vertx.core.buffer.Buffer
import io.vertx.core.http.ServerWebSocket

import io.github.pallax03.wizard.application.web.ws.ChatMessage
import io.github.pallax03.wizard.codecs.engine.lobby.LobbyCodecs.given
import io.github.pallax03.wizard.codecs.engine.model.SystemEventCodecs.given
import io.github.pallax03.wizard.codecs.syntax.CodecSyntax.*
import io.github.pallax03.wizard.codecs.ws.WsMessageCodecs.given
import io.github.pallax03.wizard.engine.lobby.{LobbyError, LobbyId, LobbyStatus}
import io.github.pallax03.wizard.engine.model.basic.PlayerId
import io.github.pallax03.wizard.engine.model.events.SystemEvent
import io.github.pallax03.wizard.engine.ports.{
  InboundPort,
  LobbyStatePort,
  PubSubPort,
  Subscription,
  WebSocketsPort
}
import io.github.pallax03.wizard.util.{ChannelsKeys, LogContext, WizardLogger}

/** Internal container tracking an active WebSocket client session and its associated resources. */
case class ClientSession(ws: ServerWebSocket, sub: Subscription, pingTimerId: Long)

object VertxWebSocketsAdapter:
  private val PING_INTERVAL_MS: Long = 20000
  private val PONG_TIMEOUT_MS: Long = 60000

/**
 * Vert.x WebSocket adapter implementing [[io.github.pallax03.wizard.engine.ports.WebSocketsPort]].
 *
 * Bridges client [[ServerWebSocket]] sessions with distributed Redis Pub/Sub channels:
 *  - Event Streaming: Subscribes clients to both broadcast and unicast game events via [[PubSubPort]].
 *  - Heartbeat & Liveness: Runs a periodic 20-second ping timer with a 60-second pong timeout to detect dead sockets.
 *  - Presence Tracking: Marks players online on connect, broadcasts [[SystemEvent.online]],
 *    and resumes paused games when applicable.
 *  - Resource Cleanup: Cancels heartbeat timers, closes subscriptions, and broadcasts [[SystemEvent.offline]] on disconnect.
 */
class VertxWebSocketsAdapter(
    val vertx: Vertx,
    val pubSubPort: PubSubPort,
    val lobbyStatePort: LobbyStatePort,
    val gameEngineInPort: InboundPort
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

    ws.textMessageHandler: text =>
      text.decodeAs[ChatMessage] match
        case Right(msg) =>
          val channel = msg.destinationId.fold(
            ChannelsKeys.pubSubLobbyChannel(lobbyId)
          )(destId => ChannelsKeys.pubSubLobbyPlayerChannel(lobbyId, destId))
          pubSubPort.publish(channel, msg.toJson)
        case Left(err) =>
          given LogContext = LogContext(lobbyId, playerId)
          WizardLogger.warn(s"Rejected malformed WebSocket message: ${err.getMessage}")
          ws.writeTextMessage(
            (LobbyError.InternalServerError("INVALID_WS_MESSAGE"): LobbyError).toJson
          )

    pubSubPort
      .subscribePlayer(lobbyId, playerId, rawJson => ws.writeTextMessage(rawJson))
      .map: sub =>
        lobbyStatePort
          .setPlayerOnlineStatus(lobbyId, playerId, true)
          .onComplete:
            case Success(Some(lobbyStatus: LobbyStatus))
                if lobbyStatus != LobbyStatus.PAUSED && lobbyStatus.existGame =>
              gameEngineInPort.resumeGame(lobbyId)
            case _ => ()
        pubSubPort.publish(
          ChannelsKeys.pubSubLobbyChannel(lobbyId),
          SystemEvent.online(playerId).toJson
        )
        sessions.put((lobbyId, playerId), ClientSession(ws, sub, setupHeartbeat(ws)))

  private def setupHeartbeat(ws: ServerWebSocket): Long =
    var lastPong = System.currentTimeMillis()
    ws.pongHandler(_ => lastPong = System.currentTimeMillis())

    vertx.setPeriodic(
      VertxWebSocketsAdapter.PING_INTERVAL_MS,
      _ =>
        if System.currentTimeMillis() - lastPong > VertxWebSocketsAdapter.PONG_TIMEOUT_MS then
          if !ws.isClosed then ws.close()
        else if !ws.isClosed then ws.writePing(Buffer.buffer("ping"))
    )

  /** @inheritdoc */
  override def close(lobbyId: LobbyId, playerId: PlayerId): Future[Unit] =
    sessions.remove((lobbyId, playerId)) match
      case Some(session) =>
        scala.util.Try(session.ws.close())
        vertx.cancelTimer(session.pingTimerId)
        session.sub.cancel()
        lobbyStatePort.setPlayerOnlineStatus(lobbyId, playerId, false).flatMap { _ =>
          pubSubPort
            .publish(ChannelsKeys.pubSubLobbyChannel(lobbyId), SystemEvent.offline(playerId).toJson)
            .void
        }
      case None =>
        Future.unit
