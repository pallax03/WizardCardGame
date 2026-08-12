package it.unibo.pps.wizard.engine.adapters.redis

import it.unibo.pps.wizard.engine.configuration.*
import it.unibo.pps.wizard.engine.lobby.LobbyId
import it.unibo.pps.wizard.engine.model.basic.*
import it.unibo.pps.wizard.engine.model.core.*
import it.unibo.pps.wizard.engine.ports.{InboundPort, OutboundPort}

import scala.concurrent.Future


class RedisInboundAdapter(
                               private val outboundPort: OutboundPort
                             ) extends InboundPort:

  print(outboundPort)
  /** @inheritdoc */
  override def getState(lobbyId: LobbyId, playerId: PlayerId): Future[GameState] =
    ???

  /** @inheritdoc */
  override def startGame(lobbyId: LobbyId, players: List[PlayerId], config: GameConfiguration): Future[Unit] =
    ???

  /** @inheritdoc */
  override def submitAction(lobbyId: LobbyId, action: GameAction): Future[Unit] =
    ???