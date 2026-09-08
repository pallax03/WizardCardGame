package io.github.pallax03.wizard.engine.adapters.redis

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import cats.syntax.all.*

import io.vertx.redis.client.Redis

import io.github.pallax03.wizard.engine.lobby.{LobbyId, LobbyPlayer}
import io.github.pallax03.wizard.engine.model.events.{DestinationScoped, InvitationEvent, LifecycleEvent, WizardEvent}
import io.github.pallax03.wizard.engine.ports.{OutboundPort, PubSubPort}
import io.github.pallax03.wizard.util.ChannelsKeys

import io.github.pallax03.wizard.codecs.syntax.CodecSyntax.*

import io.github.pallax03.wizard.codecs.engine.model.WizardEventsCodecs.given
import io.github.pallax03.wizard.codecs.engine.lobby.LobbyPlayerCodecs.given 

/**
 * Redis implementation of [[OutboundPort]].
 *
 * In addition to publishing events on the appropriate PubSub channels, this adapter
 * intercepts [[InvitationEvent]] (WaitingForCard / WaitingForBid / WaitingForTrump) to
 * schedule a turn timer on Redis. The timer key (`timer:{lobbyId}:{playerId}`) expires
 * after `config.timer + gracePeriodSeconds` seconds. A separate [[TurnTimerVerticle]]
 * listens to Redis keyspace-expired notifications and calls [[InboundPort.handleTimeout]]
 * when the key disappears without the player having played.
 */
class RedisOutboundAdapter(
    val pubSubPort: PubSubPort,
    val redisClient: Redis
) extends OutboundPort:

  /** @inheritdoc */
  override def publish(lobbyId: LobbyId, events: WizardEvent*): Future[Unit] =
    Future
      .sequence(events.map: ev =>
        val jsonMsg = ev.toJson
        pubSubPort.publish(ChannelsKeys.LOGS_CHANNEL, s"INFO:[Lobby $lobbyId] $jsonMsg")

        val publishFut = ev match
          case scoped: DestinationScoped =>
            pubSubPort.publish(
              ChannelsKeys.pubSubLobbyPlayerChannel(lobbyId, scoped.destinationId),
              jsonMsg
            )
          case _: LifecycleEvent.GameStarted | _: LifecycleEvent.GameResumed =>
            pubSubPort.publish(ChannelsKeys.SPAWN_BOT_CHANNEL, lobbyId.toString)
            pubSubPort.publish(ChannelsKeys.pubSubLobbyChannel(lobbyId), jsonMsg)
          case _ =>
            pubSubPort.publish(ChannelsKeys.pubSubLobbyChannel(lobbyId), jsonMsg)

        val turnEventFut = ev match
          case inv: InvitationEvent =>
            pubSubPort.publish(ChannelsKeys.TURN_EVENTS_CHANNEL, LobbyPlayer(lobbyId, inv.destinationId).toJson)
          case _ => Future.unit

        publishFut.zip(turnEventFut).void
      )
      .void
