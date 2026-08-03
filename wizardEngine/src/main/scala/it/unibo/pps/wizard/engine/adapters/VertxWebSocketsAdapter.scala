package it.unibo.pps.wizard.engine.adapters

import it.unibo.pps.wizard.engine.ports.WebSocketsPort
//import it.unibo.pps.wizard.engine.ports.RedisPubSubPort

import scala.concurrent.Future

class VertxWebSocketsAdapter(
                              //private val inboundPort: GameEngineInboundAdapter,
                              //private val redisPubSubPort: RedisPubSubPort
                            ) extends WebSocketsPort:
  /**
   * @inheritdoc
   *
   */
  override def handleClientMessage(jsonMessage: String): Future[Unit] =
    // Decode jsonMessage (GameAction)
//    Decode[GameAction](jsonMessage) match
//      case Right(action) => inboundPort.submitAction(action)
//      case Left(error) =>
//        Future.failed(new IllegalArgumentException(s"action not valid: ${error.getMessage}"))
    ???

  /**
   * @inheritdoc
   *
   */
  override def subscribeLobbyEvents(lobbyId: String, onJsonMessage: String): Unit = {
    // subscribe websockets to events outboundPort??? (Redis)
    ???
  }
