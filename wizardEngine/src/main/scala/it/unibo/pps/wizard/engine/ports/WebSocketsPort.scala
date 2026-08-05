package it.unibo.pps.wizard.engine.ports

import scala.concurrent.Future

/**
 * External port for the WebSockets Client used by WebSockets Verticle.
 *
 * Contain each socket for every lobby.
 */
trait WebSocketsPort:

  /**
   * Client send their game action, adapter need to codecs and handle it.
   * @param jsonMessage -> serialized GameAction to be handled by Adapter.
   */
  def handleClientMessage(jsonMessage: String): Future[Unit]

  /**
   * Subscribe clients connectd to web sockets to
   * @param lobbyId
   * @param onJsonMessage
   */
  def subscribeLobbyEvents(lobbyId: String, onJsonMessage: String): Unit
