package io.github.pallax03.wizard.application.timer

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

import cats.syntax.all.*

import io.vertx.core.AbstractVerticle
import io.vertx.redis.client.{Command, Redis, Request}

import io.github.pallax03.wizard.codecs.engine.lobby.LobbyPlayerCodecs.given
import io.github.pallax03.wizard.codecs.engine.model.SystemEventCodecs.given
import io.github.pallax03.wizard.codecs.syntax.CodecSyntax.*
import io.github.pallax03.wizard.engine.lobby.{GameConfiguration, LobbyId, LobbyPlayer, LobbyStatus}
import io.github.pallax03.wizard.engine.model.basic.PlayerId
import io.github.pallax03.wizard.engine.model.events.SystemEvent
import io.github.pallax03.wizard.engine.ports.{InboundPort, LobbyStatePort, PubSubPort}
import io.github.pallax03.wizard.util.ChannelsKeys
import io.github.pallax03.wizard.util.FutureSyntax.*

class TurnTimerVerticle(
    pubSubPort: PubSubPort,
    redisClient: Redis,
    inboundPort: InboundPort,
    lobbyStatePort: LobbyStatePort
) extends AbstractVerticle:

  override def start(): Unit =
    redisClient
      .send(
        Request.cmd(Command.create("CONFIG")).arg("SET").arg("notify-keyspace-events").arg("Ex")
      )
      .onComplete(_ => ())

    pubSubPort.subscribe(ChannelsKeys.TURN_TIMER_KEYSPACE, handleExpiredKey)
    pubSubPort.subscribe(ChannelsKeys.TURN_EVENTS_CHANNEL, handleTurnEvent)

  private def handleTurnEvent(jsonStr: String): Unit =
    (for
      payload <- Future.fromTry(jsonStr.decodeAs[LobbyPlayer].toTry)
      case Right(lobby) <- lobbyStatePort.getLobby(payload.lobbyId)
      if lobby.status == LobbyStatus.IN_GAME
      strikesResp <- redisClient
        .send(
          Request.cmd(Command.GET).arg(ChannelsKeys.afkStrikes(payload.lobbyId, payload.playerId))
        )
        .asScala
      strikes = Option(strikesResp).map(_.toString.toInt).getOrElse(0)
      ttl = Math.max(
        1,
        (lobby.configuration.timer + GameConfiguration.gracePeriodSeconds) / Math
          .pow(2, strikes)
          .toInt
      )
      _ <- redisClient
        .send(
          Request
            .cmd(Command.SET)
            .arg(ChannelsKeys.turnTimer(payload.lobbyId, payload.playerId))
            .arg("1")
            .arg("EX")
            .arg(ttl.toString)
        )
        .asScala
    yield ()).recover(_ => ())

  private def handleExpiredKey(expiredKey: String): Unit =
    expiredKey.split(':') match
      case Array("timer", lobbyIdStr, playerIdStr) =>
        val lobbyId = LobbyId(lobbyIdStr)
        val playerId = PlayerId(playerIdStr.toInt)
        (for
          case Right(lobby) <- lobbyStatePort.getLobby(lobbyId)
          if lobby.status == LobbyStatus.IN_GAME
          player = lobby.players.find(_.id == playerId).get
          _ <-
            if player.isBot then inboundPort.forceFallbackAction(lobbyId, playerId)
            else
              val strikesKey = ChannelsKeys.afkStrikes(lobbyId, playerId)
              for
                strikesResp <- redisClient.send(Request.cmd(Command.INCR).arg(strikesKey)).asScala
                _ <- redisClient
                  .send(Request.cmd(Command.EXPIRE).arg(strikesKey).arg(ChannelsKeys.DEFAULT_TTL))
                  .asScala
                _ <-
                  if strikesResp.toLong >= lobby.configuration.maxStrikes then
                    lobbyStatePort
                      .setPlayerOnlineStatus(lobbyId, playerId, false)
                      .flatMap: _ =>
                        pubSubPort
                          .publish(
                            ChannelsKeys.pubSubLobbyChannel(lobbyId),
                            SystemEvent.offline(playerId).toJson
                          )
                          .void
                  else inboundPort.forceFallbackAction(lobbyId, playerId)
              yield ()
        yield ()).recover: ex =>
          pubSubPort.publish(
            ChannelsKeys.LOGS_CHANNEL,
            s"ERROR:[TurnTimer] Failed for $lobbyIdStr/$playerIdStr: ${ex.getMessage}"
          )

      case Array("disconnect", lobbyIdStr) =>
        val lobbyId = LobbyId(lobbyIdStr)
        lobbyStatePort
          .updateLobby[(List[PlayerId], LobbyStatus)](lobbyId): lobby =>
            if lobby.status == LobbyStatus.DISCONNECTING then
              val (offlineIds, newLobby) = lobby.replaceOfflinePlayersWithBots()
              Right(((offlineIds, newLobby.status), newLobby, None))
            else Right(((Nil, lobby.status), lobby, None))
          .flatMap:
            case Right((offlineIds, newStatus)) if offlineIds.nonEmpty =>
              for
                _ <- Future.sequence(
                  offlineIds.map(pid =>
                    pubSubPort.publish(
                      ChannelsKeys.pubSubLobbyChannel(lobbyId),
                      SystemEvent.afkReplaced(pid).toJson
                    )
                  )
                )
                _ <-
                  if newStatus == LobbyStatus.IN_GAME then inboundPort.resumeGame(lobbyId)
                  else Future.unit
              yield ()
            case _ => Future.unit
          .recover: ex =>
            pubSubPort.publish(
              ChannelsKeys.LOGS_CHANNEL,
              s"ERROR:[DisconnectTimer] Failed for $lobbyIdStr: ${ex.getMessage}"
            )

      case _ => ()
