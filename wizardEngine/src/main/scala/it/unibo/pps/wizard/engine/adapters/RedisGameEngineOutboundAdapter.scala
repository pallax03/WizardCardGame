package it.unibo.pps.wizard.engine.adapters

import it.unibo.pps.wizard.engine.lobby.LobbyId
import it.unibo.pps.wizard.engine.model.events.WizardEvent
import it.unibo.pps.wizard.engine.ports.GameEngineOutboundPort
import it.unibo.pps.wizard.engine.ports.RedisPubSubPort

import scala.concurrent.Future

/**
 * Adapter that implements GameEngineOutboundPort to publish pure domain events
 * to the distributed Redis Pub/Sub infrastructure.
 *
 * This Adapter bridges the gap between the Game Engine (which knows nothing about network)
 * and Redis (which only knows strings and channels).
 *
 * @param lobbyId The UUID of the lobby this adapter is currently working for.
 * @param pubSubPort The internal port/client used to interact with Redis Pub/Sub.
 */
class RedisGameEngineOutboundAdapter(
    val lobbyId: LobbyId,
    val pubSubPort: RedisPubSubPort
) extends GameEngineOutboundPort:

  /** @inheritdoc */
  override def publishEvent(event: WizardEvent): Future[Unit] =
    // 1. Serialize the WizardEvent to a JSON `codecs.engine.events.WizardEvent`
    // 2. Check if the event is DestinationScoped or global
    ???

  /** @inheritdoc */
  override def publishAllEvents(events: List[WizardEvent]): Future[Unit] =
    ???
