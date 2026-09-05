package io.github.pallax03.wizard.engine.adapters.redis

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

import cats.syntax.all.*

import io.circe.syntax.*


import io.github.pallax03.wizard.codecs.engine.model.WizardEventsCodecs.given
import io.github.pallax03.wizard.engine.lobby.LobbyId
import io.github.pallax03.wizard.engine.model.events.{
  DestinationScoped,
  InvitationEvent,
  LifecycleEvent,
  WizardEvent
}
import io.github.pallax03.wizard.engine.ports.{LobbyStatePort, OutboundPort, PubSubPort}
import io.github.pallax03.wizard.util.ChannelsKeys

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
    val redisClient: io.vertx.redis.client.Redis,
    val lobbyStatePort: LobbyStatePort
) extends OutboundPort:

  /** @inheritdoc */
  override def publish(lobbyId: LobbyId, events: WizardEvent*): Future[Unit] =
    Future
      .sequence(events.map: ev =>
        val jsonMsg = ev.asJson.noSpaces
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

        val timerFut = ev match
          case inv: InvitationEvent => scheduleTurnTimer(lobbyId, inv)
          case _                    => Future.unit

        publishFut.zip(timerFut).void
      )
      .void

  /**
   * Reads the lobby's [[GameConfiguration]] and sets a `timer:{lobbyId}:{playerId}` key.
   */
  private def scheduleTurnTimer(lobbyId: LobbyId, inv: InvitationEvent): Future[Unit] =
    lobbyStatePort.getLobby(lobbyId).flatMap:
      case None        => Future.unit
      case Some(lobby) =>
        val ttl = lobby.configuration.timer + lobby.configuration.gracePeriodSeconds
        val req = io.vertx.redis.client.Request
          .cmd(io.vertx.redis.client.Command.SET)
          .arg(ChannelsKeys.turnTimer(lobbyId, inv.playerId))
          .arg("1")
          .arg("EX")
          .arg(ttl.toString)
        io.github.pallax03.wizard.util.FutureSyntax.asScala(redisClient.send(req)).void
