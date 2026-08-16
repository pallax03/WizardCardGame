package it.unibo.pps.wizard.engine.adapters

import io.vertx.core.http.ServerWebSocket
import it.unibo.pps.wizard.engine.lobby.LobbyId
import it.unibo.pps.wizard.engine.model.basic.PlayerId
import it.unibo.pps.wizard.engine.ports.PubSubPort
import it.unibo.pps.wizard.engine.ports.WebSocketsPort

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

import scala.collection.concurrent.TrieMap
import it.unibo.pps.wizard.engine.ports.Subscription
import scala.util.Try

case class ClientSession(ws: ServerWebSocket, lobbySub: Subscription, playerSub: Subscription)

class VertxWebSocketsAdapter(
    val pubSubPort: PubSubPort
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

    for
      lobbySub <- pubSubPort.subscribeToLobby(
        lobbyId,
        rawJson => ws.writeTextMessage(rawJson)
      )
      playerSub <- pubSubPort.subscribeToPlayer(
        lobbyId,
        playerId,
        rawJson => ws.writeTextMessage(rawJson)
      )
    yield 
      sessions.put((lobbyId, playerId), ClientSession(ws, lobbySub, playerSub))
      ()

  /** @inheritdoc */
  override def close(lobbyId: LobbyId, playerId: PlayerId): Future[Unit] =
    sessions.remove((lobbyId, playerId)) match
      case Some(session) =>
        Try(session.ws.close())
        for
          _ <- session.lobbySub.cancel()
          _ <- session.playerSub.cancel()
        yield ()
      case None =>
        Future.unit
