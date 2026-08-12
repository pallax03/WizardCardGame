package it.unibo.pps.wizard.engine.adapters.redis

import it.unibo.pps.wizard.engine.lobby.LobbyId
import it.unibo.pps.wizard.engine.model.events.WizardEvent
import it.unibo.pps.wizard.engine.ports.GameEngineOutboundPort
import it.unibo.pps.wizard.engine.ports.PubSubPort

import scala.concurrent.Future

/**
 * Adapter that implements GameEngineOutboundPort to publish pure domain events
 * to the distributed Redis Pub/Sub infrastructure.
 *
 * This Adapter bridges the gap between the Game Engine (which knows nothing about network)
 * and Redis (which only knows strings and channels).
 *
 * @param pubSubPort The internal port/client used to interact with Redis Pub/Sub.
 */
class RedisGameEngineOutboundAdapter(
    val pubSubPort: PubSubPort
) extends GameEngineOutboundPort:

  /** @inheritdoc */
  override def publish(lobbyId: LobbyId, events: WizardEvent*): Future[Unit] =
    // 1. Serialize the WizardEvent to a JSON `codecs.engine.events.WizardEvent`
    // 2. Check if the event is DestinationScoped or global
    ???
