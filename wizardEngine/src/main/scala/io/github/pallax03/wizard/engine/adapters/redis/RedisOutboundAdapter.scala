package io.github.pallax03.wizard.engine.adapters.redis

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

import cats.syntax.all.*

import io.circe.syntax.*

import io.github.pallax03.wizard.codecs.engine.model.WizardEventsCodecs.given
import io.github.pallax03.wizard.engine.lobby.LobbyId
import io.github.pallax03.wizard.engine.model.events.{
  DestinationScoped,
  LifecycleEvent,
  WizardEvent
}
import io.github.pallax03.wizard.engine.ports.{OutboundPort, PubSubPort}
import io.github.pallax03.wizard.util.ChannelsKeys

class RedisOutboundAdapter(
    val pubSubPort: PubSubPort
) extends OutboundPort:

  /** @inheritdoc */
  override def publish(lobbyId: LobbyId, events: WizardEvent*): Future[Unit] =
    Future
      .sequence(events.map: ev =>
        val jsonMsg = ev.asJson.noSpaces
        pubSubPort.publish(ChannelsKeys.LOGS_CHANNEL, s"INFO:[Lobby $lobbyId] $jsonMsg")
        
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
