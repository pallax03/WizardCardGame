package io.github.pallax03.wizard.application.timer

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Success
import io.vertx.core.AbstractVerticle
import io.vertx.redis.client.{Command, Redis, Request}
import io.github.pallax03.wizard.codecs.engine.lobby.LobbyPlayerCodecs.given
import io.github.pallax03.wizard.codecs.syntax.CodecSyntax.*
import io.github.pallax03.wizard.engine.lobby.{LobbyId, LobbyPlayer, LobbyStatus}
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
    jsonStr.decodeAs[LobbyPlayer] match
      case Right(payload) =>
        val lobbyId = payload.lobbyId
        val playerId = payload.playerId
        lobbyStatePort
          .getLobby(lobbyId)
          .onComplete:
            case Success(Right(lobby)) =>
              val strikesKey = ChannelsKeys.afkStrikes(lobbyId, playerId)
              redisClient
                .send(Request.cmd(Command.GET).arg(strikesKey))
                .asScala
                .onComplete:
                  case Success(strikesResp) =>
                    val strikes = Option(strikesResp).map(_.toString.toInt).getOrElse(0)
                    val baseTtl = lobby.configuration.timer + lobby.configuration.gracePeriodSeconds
                    val ttl = Math.max(1, baseTtl / Math.pow(2, strikes).toInt)
                    val req = Request
                      .cmd(Command.SET)
                      .arg(ChannelsKeys.turnTimer(lobbyId, playerId))
                      .arg("1")
                      .arg("EX")
                      .arg(ttl.toString)
                    redisClient.send(req)
                  case _ => ()
            case _ => ()
      case Left(_) => ()

  private def handleExpiredKey(expiredKey: String): Unit =
    expiredKey.split(':') match
      case Array("timer", lobbyIdStr, playerIdStr) =>
        val lobbyId = LobbyId(lobbyIdStr)
        val playerId = PlayerId(playerIdStr.toInt)
        val strikesKey = ChannelsKeys.afkStrikes(lobbyId, playerId)
        (for
          eitherLobby <- lobbyStatePort.getLobby(lobbyId)
          lobby <- Future.successful(eitherLobby.toOption.get)
          isOffline = !lobby.players.find(_.id == playerId).exists(_.isOnline)
          strikesResp <- redisClient.send(Request.cmd(Command.INCR).arg(strikesKey)).asScala
          strikes = strikesResp.toLong
          _ <- redisClient.send(Request.cmd(Command.EXPIRE).arg(strikesKey).arg("86400")).asScala
          _ <-
            if lobby.status == LobbyStatus.PAUSED then Future.unit
            else if isOffline || strikes >= lobby.configuration.maxStrikes then
              lobbyStatePort.updateLobby[Unit](lobbyId) { l =>
                  val newPlayers = l.players.map(p =>
                    if p.id == playerId then p.replaceWithABot()
                    else p
                  )
                  Right(((), l.copy(players = newPlayers), Option(SystemEvent.timeout(playerId))))
              }.flatMap(_ => inboundPort.forceFallbackAction(lobbyId, playerId))
            else inboundPort.forceFallbackAction(lobbyId, playerId)
        yield ()).recover:
          case ex =>
            pubSubPort.publish(
              ChannelsKeys.LOGS_CHANNEL,
              s"ERROR:[TurnTimer] Failed for $lobbyIdStr/$playerIdStr: ${ex.getMessage}"
            )
      case _ => ()
