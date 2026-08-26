package io.github.pallax03.wizard.engine.adapters.redis

import cats.syntax.all._
import io.circe.syntax._
import io.github.pallax03.wizard.codecs.engine.model.WizardEventsCodecs.given
import io.github.pallax03.wizard.engine.lobby.LobbyId
import io.github.pallax03.wizard.engine.model.events.DestinationScoped
import io.github.pallax03.wizard.engine.model.events.LifecycleEvent
import io.github.pallax03.wizard.engine.model.events.WizardEvent
import io.github.pallax03.wizard.engine.ports.OutboundPort
import io.github.pallax03.wizard.engine.ports.PubSubPort
import io.github.pallax03.wizard.util.ChannelsKeys

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
      .void
