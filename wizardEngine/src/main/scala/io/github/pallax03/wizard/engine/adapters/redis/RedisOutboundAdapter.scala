package io.github.pallax03.wizard.engine.adapters.redis

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

import cats.syntax.all.*

import io.vertx.redis.client.{Command, Redis, Request}

import io.github.pallax03.wizard.codecs.engine.lobby.BotTaskCodecs.given
import io.github.pallax03.wizard.codecs.engine.lobby.LobbyPlayerCodecs.given
import io.github.pallax03.wizard.codecs.engine.model.WizardEventsCodecs.given
import io.github.pallax03.wizard.codecs.syntax.CodecSyntax.*
import io.github.pallax03.wizard.engine.lobby.{BotTask, LobbyId, LobbyPlayer, LobbyStatus}
import io.github.pallax03.wizard.engine.model.events.{
  DestinationScoped,
  InvitationEvent,
  LifecycleEvent,
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
 * The consumer reads one string, decodes it with Circe.
 */
class RedisOutboundAdapter(
    val pubSubPort: PubSubPort,
    val redisClient: Redis,
    val lobbyStatePort: LobbyStatePort
) extends OutboundPort:

  /** @inheritdoc */
  override def publish(lobbyId: LobbyId, events: WizardEvent*): Future[Unit] =
    Future
      .traverse(events.toList): ev =>
        val jsonMsg = ev.toJson
        pubSubPort.publish(ChannelsKeys.LOGS_CHANNEL, s"INFO:[Lobby $lobbyId] $jsonMsg")

        for
          _ <- publishToClients(lobbyId, ev, jsonMsg)
          _ <- maybeDispatchBotTask(lobbyId, ev)
          _ <- maybeStartTurnTimer(lobbyId, ev)
          _ <- maybeUpdateLobbyStatus(lobbyId, ev)
        yield ()
      .void

  private def publishToClients(lobbyId: LobbyId, ev: WizardEvent, jsonMsg: String): Future[Unit] =
    ev match
      case scoped: DestinationScoped =>
        pubSubPort.publish(
          ChannelsKeys.pubSubLobbyPlayerChannel(lobbyId, scoped.destinationId),
          jsonMsg
        )
      case _ =>
        pubSubPort.publish(ChannelsKeys.pubSubLobbyChannel(lobbyId), jsonMsg)

  private def maybeDispatchBotTask(lobbyId: LobbyId, ev: WizardEvent): Future[Unit] =
    ev match
      case inv: InvitationEvent =>
        lobbyStatePort
          .getLobby(lobbyId)
          .flatMap:
            case Right(lobby) if lobby.players.exists(p => p.id == inv.destinationId && p.isBot) =>
              val taskJson = BotTask(lobbyId, inv).toJson
              val req = Request
                .cmd(Command.XADD)
                .arg(ChannelsKeys.BOT_TASKS_STREAM)
                .arg("*")
                .arg("data")
                .arg(taskJson)
              redisClient.send(req).asScala.void
            case _ => Future.unit
      case _ => Future.unit

  private def maybeStartTurnTimer(lobbyId: LobbyId, ev: WizardEvent): Future[Unit] =
    ev match
      case inv: InvitationEvent =>
        pubSubPort.publish(
          ChannelsKeys.TURN_EVENTS_CHANNEL,
          LobbyPlayer(lobbyId, inv.destinationId).toJson
        )
      case _ => Future.unit

  private def maybeUpdateLobbyStatus(lobbyId: LobbyId, ev: WizardEvent): Future[Unit] =
    ev match
      case _: LifecycleEvent.GameEnded =>
        lobbyStatePort
          .updateLobby[Unit](lobbyId) { lobby =>
            Right(((), lobby.copy(status = LobbyStatus.FINISHED), None))
          }
          .void
      case _: LifecycleEvent.GameCancelled =>
        lobbyStatePort
          .updateLobby[Unit](lobbyId): lobby =>
            Right(((), lobby.copy(status = LobbyStatus.WAITING), None))
          .void
      case _ => Future.unit
