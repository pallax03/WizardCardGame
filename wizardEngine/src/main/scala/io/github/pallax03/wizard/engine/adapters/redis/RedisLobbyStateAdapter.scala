package io.github.pallax03.wizard.engine.adapters.redis

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

import cats.syntax.all.*

import io.vertx.redis.client.{Command, Redis, Request}

import io.github.pallax03.wizard.codecs.engine.lobby.LobbyCodecs.given
import io.github.pallax03.wizard.codecs.engine.model.SystemEventCodecs.given
import io.github.pallax03.wizard.codecs.syntax.CodecSyntax.*
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
  override def addPlayer(
      lobbyId: LobbyId,
      name: String,
      difficulty: Option[BotsDifficulty]
  ): Future[Either[LobbyError, Player]] =
    val req = Request
      .cmd(Command.EVAL)
      .arg(RedisLobbyScripts.addPlayerScript)
      .arg("1")
      .arg(ChannelsKeys.lobby(lobbyId))
      .arg(name)
      .arg(difficulty.map(_.toString).getOrElse(""))
      .arg(lobbyId.toString)

    redisClient
      .send(req)
      .asScala
      .map:
        case null => Left(LobbyError.LobbyFull)
        case response =>
          response.toString match
            case "ERR_IN_PROGRESS" => Left(LobbyError.GameInProgress)
            case "ERR_FULL"        => Left(LobbyError.LobbyFull)
            case "ERR_TAKEN"       => Left(LobbyError.NameTaken)
            case json              => Right(json.decodeAs[Player].toOption.get)
      .flatMap:
        case Right(player) =>
          val msg = SystemEvent.joined(player.id).toJson
          redisClient
            .send(Request.cmd(Command.PUBLISH).arg(ChannelsKeys.pubSubLobbyChannel(lobbyId)).arg(msg))
            .asScala
            .map(_ => Right(player))
        case Left(error) => Future.successful(Left(error))

  /** @inheritdoc */
  override def removePlayer(lobbyId: LobbyId, playerId: PlayerId): Future[Boolean] =
    val req = Request
      .cmd(Command.EVAL)
      .arg(RedisLobbyScripts.removePlayerScript)
      .arg("1")
      .arg(ChannelsKeys.lobby(lobbyId))
      .arg(playerId.toInt.toString)

    redisClient
      .send(req)
      .asScala
      .flatMap: resp =>
        if resp != null && resp.toInteger == 1 then
          val msg = SystemEvent.left(playerId).toJson
          redisClient
            .send(Request.cmd(Command.PUBLISH).arg(ChannelsKeys.pubSubLobbyChannel(lobbyId)).arg(msg))
            .asScala
            .as(true)
        else
          Future.successful(false)

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
    val req = Request
      .cmd(Command.EVAL)
      .arg(RedisLobbyScripts.setPlayerOnlineScript)
      .arg("1")
      .arg(ChannelsKeys.lobby(lobbyId))
      .arg(playerId.toInt.toString)
      .arg(isOnline.toString)
    redisClient.send(req).asScala.map(resp => resp != null && resp.toInteger == 1)

  /** @inheritdoc */
  override def tryAcquireBotLock(
      lobbyId: LobbyId,
      podId: String,
      ttlSeconds: Long = 30
  ): Future[Boolean] =
    val req = Request
      .cmd(Command.SET)
      .arg(ChannelsKeys.botLock(lobbyId))
      .arg(podId)
      .arg("NX")
      .arg("EX")
      .arg(ttlSeconds.toString)
    redisClient.send(req).asScala.map(_ != null)
