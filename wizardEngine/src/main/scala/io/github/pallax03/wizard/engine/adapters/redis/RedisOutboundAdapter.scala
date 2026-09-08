package io.github.pallax03.wizard.engine.adapters.redis

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

import cats.syntax.all.*

import io.vertx.redis.client.{Command, Redis, Request}

import io.github.pallax03.wizard.codecs.engine.lobby.BotTaskCodecs.given
import io.github.pallax03.wizard.codecs.engine.lobby.LobbyPlayerCodecs.given
import io.github.pallax03.wizard.codecs.engine.model.WizardEventsCodecs.given
import io.github.pallax03.wizard.codecs.syntax.CodecSyntax.*
import io.github.pallax03.wizard.engine.lobby.{BotTask, LobbyId, LobbyPlayer}
import io.github.pallax03.wizard.engine.model.events.{
  DestinationScoped,
  InvitationEvent,
  WizardEvent
}
import io.github.pallax03.wizard.engine.ports.{LobbyStatePort, OutboundPort, PubSubPort}
import io.github.pallax03.wizard.util.ChannelsKeys
import io.github.pallax03.wizard.util.FutureSyntax.*

/**
 * Redis implementation of [[OutboundPort]].
 *
 * For [[InvitationEvent]]s targeting a bot, pushes a [[BotTask]] (serialized as JSON)
 * into a single `data` field on the Redis Stream `bot:tasks`.
 * The consumer reads one string, decodes it with circe — no RESP2/RESP3 field-index gymnastics.
 */
class RedisOutboundAdapter(
    val pubSubPort: PubSubPort,
    val redisClient: Redis,
    val lobbyStatePort: LobbyStatePort
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
          case _ =>
            pubSubPort.publish(ChannelsKeys.pubSubLobbyChannel(lobbyId), jsonMsg)

        val botTaskFut = ev match
          case inv: InvitationEvent =>
            lobbyStatePort
              .getLobby(lobbyId)
              .flatMap:
                case Right(lobby)
                    if lobby.players
                      .exists(p => p.id == inv.destinationId && p.isBot) =>
                  val taskJson = BotTask(lobbyId, inv).toJson
                  redisClient
                    .send(
                      Request
                        .cmd(Command.XADD)
                        .arg(ChannelsKeys.BOT_TASKS_STREAM)
                        .arg("*")
                        .arg("data")
                        .arg(taskJson)
                    )
                    .asScala
                    .void
                case _ => Future.unit
          case _ => Future.unit

        val turnEventFut = ev match
          case inv: InvitationEvent =>
            pubSubPort.publish(
              ChannelsKeys.TURN_EVENTS_CHANNEL,
              LobbyPlayer(lobbyId, inv.destinationId).toJson
            )
          case _ => Future.unit

        publishFut.zip(botTaskFut).zip(turnEventFut).void
      )
      .void
