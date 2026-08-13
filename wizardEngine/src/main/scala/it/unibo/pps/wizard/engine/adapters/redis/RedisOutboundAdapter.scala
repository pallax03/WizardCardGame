package it.unibo.pps.wizard.engine.adapters.redis

import it.unibo.pps.wizard.engine.lobby.LobbyId
import it.unibo.pps.wizard.engine.model.events.{WizardEvent, DestinationScoped}
import it.unibo.pps.wizard.engine.ports.OutboundPort
import it.unibo.pps.wizard.engine.ports.PubSubPort

import scala.concurrent.{Future, ExecutionContext}
import it.unibo.pps.wizard.codecs.engine.model.WizardEventsCodecs.given
import io.circe.syntax.*

import scala.concurrent.ExecutionContext.Implicits.global

class RedisOutboundAdapter(
    val pubSubPort: PubSubPort
) extends OutboundPort:

  /** @inheritdoc */
  override def publish(lobbyId: LobbyId, events: WizardEvent*): Future[Unit] =
    Future.sequence(events.map { ev =>
      val jsonMsg = ev.asJson.noSpaces
      ev match
        case scoped: DestinationScoped =>
          pubSubPort.publish(RedisKeys.pubLobbyPlayerChannel(lobbyId, scoped.destinationId), jsonMsg)
        case _ =>
          pubSubPort.publish(RedisKeys.pubLobbyChannel(lobbyId), jsonMsg)
    }).map(_ => ())