package io.github.pallax03.wizard.application.timer

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Success
import cats.syntax.all.*
import io.vertx.core.AbstractVerticle
import io.vertx.redis.client.{Command, Redis, Request}
import io.github.pallax03.wizard.codecs.engine.lobby.LobbyPlayerCodecs.given
import io.github.pallax03.wizard.codecs.syntax.CodecSyntax.*
import io.github.pallax03.wizard.codecs.engine.model.SystemEventCodecs.given
import io.github.pallax03.wizard.engine.lobby.{GameConfiguration, LobbyError, LobbyId, LobbyPlayer, LobbyStatus}
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
                    val baseTtl = lobby.configuration.timer + GameConfiguration.gracePeriodSeconds
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
          player = lobby.players.find(_.id == playerId).get
          strikesResp <- redisClient.send(Request.cmd(Command.INCR).arg(strikesKey)).asScala
          _ <- redisClient.send(Request.cmd(Command.EXPIRE).arg(strikesKey).arg("86400")).asScala
          _ <-
            if lobby.status == LobbyStatus.PAUSED then Future.unit
            else if player.isBot then
              inboundPort.forceFallbackAction(lobbyId, playerId)
            else if strikesResp.toLong >= lobby.configuration.maxStrikes then
              lobbyStatePort.setPlayerOnlineStatus(lobbyId, playerId, false).flatMap { _ =>
                val msg = SystemEvent.offline(playerId).toJson
                pubSubPort.publish(ChannelsKeys.pubSubLobbyChannel(lobbyId), msg).void
              }
            else inboundPort.forceFallbackAction(lobbyId, playerId)
        yield ()).recover:
          case ex =>
            pubSubPort.publish(ChannelsKeys.LOGS_CHANNEL, s"ERROR:[TurnTimer] Failed for $lobbyIdStr/$playerIdStr: ${ex.getMessage}")

      case Array("disconnect", lobbyIdStr) =>
        val lobbyId = LobbyId(lobbyIdStr)
        lobbyStatePort.updateLobby[List[PlayerId]](lobbyId) { lobby =>
           if lobby.status == LobbyStatus.PAUSED then
             val humans = lobby.players.filter(_.isHumanPlaying)
             if humans.nonEmpty && !humans.forall(_.isOnline) then
               val offlinePlayerIds = lobby.players.filter(p => p.isHumanPlaying && !p.isOnline).map(_.id)
               val newPlayers = lobby.players.map(p => if offlinePlayerIds.contains(p.id) then p.replaceWithABot() else p)
               Right((offlinePlayerIds, lobby.copy(players = newPlayers, status = LobbyStatus.IN_GAME), None))
             else Left(LobbyError.GameInProgress)
           else Left(LobbyError.GameInProgress)
        }.flatMap {
           case Right(offlinePlayerIds) =>
             Future.sequence(offlinePlayerIds.map { pid =>
               pubSubPort.publish(ChannelsKeys.pubSubLobbyChannel(lobbyId), SystemEvent.timeout(pid).toJson)
             }).flatMap(_ => inboundPort.resumeGame(lobbyId))
           case Left(_) => Future.unit
        }.recover {
           case ex => pubSubPort.publish(ChannelsKeys.LOGS_CHANNEL, s"ERROR:[DisconnectTimer] Failed for $lobbyIdStr: ${ex.getMessage}")
        }
      case _ => ()
