package io.github.pallax03.wizard.engine.adapters.redis

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

import cats.syntax.all.*

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
  override def saveLobby(lobby: Lobby): Future[Unit] =
    val req = Request
      .cmd(Command.SET)
      .arg(ChannelsKeys.lobby(lobby.uuid))
      .arg(lobby.toJson)
      .arg("EX")
      .arg("86400")
    redisClient.send(req).asScala.void

  /** @inheritdoc */
  override def getLobby(lobbyId: LobbyId): Future[Option[Lobby]] =
    val req = Request.cmd(Command.GET).arg(ChannelsKeys.lobby(lobbyId))
    redisClient
      .send(req)
      .asScala
      .map:
        case null     => None
        case response => response.toString.decodeAs[Lobby].toOption

  /** @inheritdoc */
  private def updateLobbyCAS[A](lobbyId: LobbyId)(
      f: Option[Lobby] => Either[LobbyError, (A, Lobby, Option[SystemEvent])]
  ): Future[Either[LobbyError, A]] =
    getLobby(lobbyId).flatMap: optLobby =>
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
              else updateLobbyCAS(lobbyId)(f)

  /** @inheritdoc */
  override def addPlayer(
      lobbyId: LobbyId,
      name: String,
      difficulty: Option[BotsDifficulty],
      secret: Option[String] = None
  ): Future[Either[LobbyError, Player]] =
    updateLobbyCAS(lobbyId): optLobby =>
      val lobby =
        optLobby.getOrElse(Lobby(lobbyId, List.empty, LobbyStatus.WAITING, GameConfiguration(), 0))
      lobby
        .addPlayer(name, difficulty, secret)
        .map:
          case (p, l) => (p, l, Some(SystemEvent.joined(p.id)))

  /** @inheritdoc */
  override def removePlayer(lobbyId: LobbyId, playerId: PlayerId): Future[Boolean] =
    updateLobbyCAS[Boolean](lobbyId) {
      case None => Left(LobbyError.LobbyNotFound)
      case Some(lobby) =>
        lobby
          .removePlayer(playerId)
          .map(newLobby => (true, newLobby, Some(SystemEvent.left(playerId))))
    }.map(_.getOrElse(false))

  /** @inheritdoc */
  override def getAllLobbies: Future[List[Lobby]] =
    redisClient
      .send(Request.cmd(Command.KEYS).arg(ChannelsKeys.LOBBY_CHANNEL))
      .asScala
      .flatMap:
        case null => Future.successful(List.empty)
        case keysResp =>
          import scala.jdk.CollectionConverters.*
          val keys = keysResp.asScala.map(_.toString).toList
          if keys.isEmpty then Future.successful(List.empty)
          else
            val getReq = Request.cmd(Command.MGET)
            keys.foreach(getReq.arg)
            redisClient
              .send(getReq)
              .asScala
              .map:
                case null => List.empty
                case valsResp =>
                  valsResp.asScala
                    .flatMap(v => if v != null then v.toString.decodeAs[Lobby].toOption else None)
                    .toList

  /** @inheritdoc */
  override def setPlayerOnlineStatus(
      lobbyId: LobbyId,
      playerId: PlayerId,
      isOnline: Boolean
  ): Future[Boolean] =
    updateLobbyCAS[Boolean](lobbyId) {
      case None => Left(LobbyError.LobbyNotFound)
      case Some(lobby) =>
        lobby.setPlayerOnlineStatus(playerId, isOnline).map(newLobby => (true, newLobby, None))
    }.map(_.getOrElse(false))

  override def disconnectAndPauseLobby(lobbyId: LobbyId, playerId: PlayerId): Future[Boolean] =
    updateLobbyCAS[Boolean](lobbyId) {
      case None => Left(LobbyError.LobbyNotFound)
      case Some(lobby) =>
        lobby.setPlayerOnlineStatus(playerId, false).map { newLobby =>
          val pausedLobby =
            if newLobby.status == LobbyStatus.IN_GAME then
              newLobby.copy(status = LobbyStatus.PAUSED)
            else newLobby
          (true, pausedLobby, Some(SystemEvent.offline(playerId)))
        }
    }.flatMap {
      case Left(_) => Future.successful(false)
      case Right(res) =>
        getLobby(lobbyId).flatMap {
          case None => Future.successful(res)
          case Some(lobby) =>
            val keysToDelete = lobby.players.flatMap { p =>
              List(ChannelsKeys.turnTimer(lobbyId, p.id), ChannelsKeys.afkStrikes(lobbyId, p.id))
            }
            if keysToDelete.nonEmpty then
              val delReq = Request.cmd(Command.DEL)
              keysToDelete.foreach(delReq.arg)
              redisClient.send(delReq).asScala.map(_ => res)
            else Future.successful(res)
        }
    }
