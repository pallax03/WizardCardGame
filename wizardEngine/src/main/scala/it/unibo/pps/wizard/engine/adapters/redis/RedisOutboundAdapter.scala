package it.unibo.pps.wizard.engine.adapters.redis

import io.circe.syntax._
import it.unibo.pps.wizard.codecs.engine.model.WizardEventsCodecs.given
import it.unibo.pps.wizard.engine.lobby.LobbyId
import it.unibo.pps.wizard.engine.model.events.DestinationScoped
import it.unibo.pps.wizard.engine.model.events.LifecycleEvent
import it.unibo.pps.wizard.engine.model.events.WizardEvent
import it.unibo.pps.wizard.engine.ports.OutboundPort
import it.unibo.pps.wizard.engine.ports.PubSubPort
import it.unibo.pps.wizard.util.ChannelsKeys

import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class RedisOutboundAdapter(
    val pubSubPort: PubSubPort
) extends OutboundPort:

  /** @inheritdoc */
  override def publish(lobbyId: LobbyId, events: WizardEvent*): Future[Unit] =
    Future
      .sequence(events.map: ev =>
        val jsonMsg = ev.asJson.noSpaces
        ev match
          case scoped: DestinationScoped =>
            pubSubPort.publish(
              ChannelsKeys.pubSubLobbyPlayerChannel(lobbyId, scoped.destinationId),
              jsonMsg
            )
          case _: LifecycleEvent.GameStarted =>
            pubSubPort.publish(ChannelsKeys.SPAWN_BOT_CHANNEL, lobbyId.toString)
            pubSubPort.publish(ChannelsKeys.pubSubLobbyChannel(lobbyId), jsonMsg)
          case _ =>
            pubSubPort.publish(ChannelsKeys.pubSubLobbyChannel(lobbyId), jsonMsg)
      )
      .map(_ => ())
