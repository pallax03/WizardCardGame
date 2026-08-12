package it.unibo.pps.wizard.engine.adapters.redis

import it.unibo.pps.wizard.engine.lobby.LobbyId
import it.unibo.pps.wizard.engine.model.events.WizardEvent
import it.unibo.pps.wizard.engine.ports.OutboundPort
import it.unibo.pps.wizard.engine.ports.PubSubPort

import scala.concurrent.Future


class RedisOutboundAdapter(
    val pubSubPort: PubSubPort
) extends OutboundPort:

  /** @inheritdoc */
  override def publish(lobbyId: LobbyId, events: WizardEvent*): Future[Unit] =
    // 1. Serialize the WizardEvent to a JSON `codecs.engine.events.WizardEvent`
    // 2. Check if the event is DestinationScoped or global
    ???
