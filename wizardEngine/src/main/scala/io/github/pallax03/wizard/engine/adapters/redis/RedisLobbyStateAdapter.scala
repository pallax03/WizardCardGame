package io.github.pallax03.wizard.engine.adapters.redis

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

import io.vertx.redis.client.{Command, Redis, Request}

import io.github.pallax03.wizard.codecs.engine.lobby.LobbyCodecs.given
import io.github.pallax03.wizard.codecs.engine.model.SystemEventCodecs.given
import io.github.pallax03.wizard.codecs.syntax.CodecSyntax.*
import io.github.pallax03.wizard.engine.configuration.GameConfiguration
import io.github.pallax03.wizard.engine.lobby.*
import io.github.pallax03.wizard.engine.model.basic.PlayerId
import io.github.pallax03.wizard.engine.model.events.SystemEvent
import io.github.pallax03.wizard.engine.ports.LobbyStatePort
import io.github.pallax03.wizard.util.ChannelsKeys
import io.github.pallax03.wizard.util.FutureSyntax.*

class RedisLobbyStateAdapter(redisClient: Redis) extends LobbyStatePort:

  /** @inheritdoc */
  override def getLobby(lobbyId: LobbyId): Future[Either[LobbyError, Lobby]] =
    val req = Request.cmd(Command.GET).arg(ChannelsKeys.lobby(lobbyId))
    redisClient
      .send(req)
      .asScala
      .map:
        case null     => Left(LobbyError.LobbyNotFound)
        case response => response.toString.decodeAs[Lobby].left.map(_ => LobbyError.LobbyNotFound)

  /** @inheritdoc */
  override def getAuthLobby(
      lobbyId: LobbyId,
      secret: String
  ): Future[Either[LobbyError, (Player, Lobby)]] =
    getLobby(lobbyId).map:
      case Right(lobby) => lobby.authenticate(secret).map(player => (player, lobby))
      case Left(err)    => Left(err)

  private def upsertLobby[A](lobbyId: LobbyId)(
      f: Option[Lobby] => Either[LobbyError, (A, Lobby, Option[SystemEvent])]
  ): Future[Either[LobbyError, A]] =
    getLobby(lobbyId).flatMap: eitherLobby =>
      val optLobby = eitherLobby.toOption
      val expectedVersion = optLobby.map(_.version).getOrElse(0)
      f(optLobby) match
        case Left(err) => Future.successful(Left(err))
        case Right((res, newLobby, eventOpt)) =>
          val req = Request
            .cmd(Command.EVAL)
            .arg(RedisLobbyScripts.casLobbyScript)
            .arg("1")
            .arg(ChannelsKeys.lobby(lobbyId))
            .arg(expectedVersion.toString)
            .arg(newLobby.toJson)
          redisClient
            .send(req)
            .asScala
            .flatMap: resp =>
              if resp.toBoolean then
                eventOpt match
                  case Some(ev) =>
                    redisClient
                      .send(
                        Request
                          .cmd(Command.PUBLISH)
                          .arg(ChannelsKeys.pubSubLobbyChannel(lobbyId))
                          .arg(ev.toJson)
                      )
                      .asScala
                      .map(_ => Right(res))
                  case None => Future.successful(Right(res))
              else upsertLobby(lobbyId)(f)

  /** @inheritdoc */
  override def updateLobby[A](lobbyId: LobbyId)(
      f: Lobby => Either[LobbyError, (A, Lobby, Option[SystemEvent])]
  ): Future[Either[LobbyError, A]] =
    upsertLobby(lobbyId):
      case Some(lobby) => f(lobby)
      case None        => Left(LobbyError.LobbyNotFound)

  /** @inheritdoc */
  override def addPlayer(
      lobbyId: LobbyId,
      name: String,
      difficulty: Option[BotsDifficulty],
      secret: Option[String] = None
  ): Future[Either[LobbyError, Player]] =
    upsertLobby(lobbyId): optLobby =>
      val lobby =
        optLobby.getOrElse(Lobby(lobbyId, List.empty, LobbyStatus.WAITING, GameConfiguration(), 0))
      lobby
        .addPlayer(name, difficulty, secret)
        .map:
          case (p, l) => (p, l, Some(SystemEvent.joined(p.id)))

  /** @inheritdoc */
  override def setPlayerOnlineStatus(
      lobbyId: LobbyId,
      playerId: PlayerId,
      isOnline: Boolean
  ): Future[LobbyStatus] =
    updateLobby[Lobby](lobbyId) { lobby =>
        lobby.handleOnlineStatusChange(playerId, isOnline).map(newLobby => (newLobby, newLobby, None))
    }.flatMap {
      case Left(_) => Future.failed(new Exception("Player or Lobby not found"))
      case Right(newLobby) =>
        if isOnline then
          redisClient.send(Request.cmd(Command.DEL).arg(ChannelsKeys.afkStrikes(lobbyId, playerId))).asScala.flatMap { _ =>
            val humans = newLobby.players.filter(_.isHumanPlaying)
            if humans.isEmpty || humans.forall(_.isOnline) then
              redisClient.send(Request.cmd(Command.DEL).arg(ChannelsKeys.disconnectTimer(lobbyId))).asScala.map(_ => newLobby.status)
            else Future.successful(newLobby.status)
          }
        else
          val humans = newLobby.players.filter(_.isHumanPlaying)
          if humans.nonEmpty && !humans.forall(_.isOnline) then
             val req = Request.cmd(Command.SET).arg(ChannelsKeys.disconnectTimer(lobbyId)).arg("1").arg("EX").arg(newLobby.configuration.timer.toString)
             redisClient.send(req).asScala.map(_ => newLobby.status)
          else Future.successful(newLobby.status)
    }
