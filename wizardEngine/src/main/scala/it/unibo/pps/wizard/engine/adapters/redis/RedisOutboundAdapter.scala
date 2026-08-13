package it.unibo.pps.wizard.engine.adapters.redis

import it.unibo.pps.wizard.engine.lobby.LobbyId
import it.unibo.pps.wizard.engine.model.events.{DestinationScoped, WizardEvent, LifecycleEvent}
import it.unibo.pps.wizard.engine.ports.OutboundPort
import it.unibo.pps.wizard.engine.ports.PubSubPort

import scala.concurrent.{ExecutionContext, Future}
import it.unibo.pps.wizard.codecs.engine.model.WizardEventsCodecs.given
import io.circe.syntax.*
import it.unibo.pps.wizard.util.ChannelsKeys

import scala.concurrent.ExecutionContext.Implicits.global

class RedisOutboundAdapter(
    val pubSubPort: PubSubPort
) extends OutboundPort:

  /** @inheritdoc */
  override def publish(lobbyId: LobbyId, events: WizardEvent*): Future[Unit] =
    Future.sequence(events.map: ev =>
      val jsonMsg = ev.asJson.noSpaces
      ev match
        case scoped: DestinationScoped =>
          pubSubPort.publish(ChannelsKeys.pubSubLobbyPlayerChannel(lobbyId, scoped.destinationId), jsonMsg)
        case _: LifecycleEvent.GameStarted =>
          pubSubPort.publish(ChannelsKeys.SPAWN_BOT_CHANNEL, lobbyId.toString)
          pubSubPort.publish(ChannelsKeys.pubSubLobbyChannel(lobbyId), jsonMsg)
        case _ =>
          pubSubPort.publish(ChannelsKeys.pubSubLobbyChannel(lobbyId), jsonMsg)
    ).map(_ => ())