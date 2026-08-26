package it.unibo.pps.wizard.engine.adapters.redis

import cats.syntax.all._
import io.vertx.redis.client.Command
import io.vertx.redis.client.Redis
import io.vertx.redis.client.Request
import it.unibo.pps.wizard.codecs.engine.lobby.LobbyCodecs.given
import it.unibo.pps.wizard.codecs.syntax.CodecSyntax._
import it.unibo.pps.wizard.engine.lobby._
import it.unibo.pps.wizard.engine.model.basic.PlayerId
import it.unibo.pps.wizard.engine.ports.LobbyStatePort
import it.unibo.pps.wizard.util.ChannelsKeys
import it.unibo.pps.wizard.util.FutureSyntax._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

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

  private val addPlayerScript =
    """
    |local lobbyStr = redis.call('GET', KEYS[1])
    |local lobby
    |if not lobbyStr then
    |  lobby = { lobbyId = ARGV[3], players = {}, status = "WAITING" }
    |else
    |  lobby = cjson.decode(lobbyStr)
    |end
    |
    |local inputName = ARGV[1]
    |local isBot = ARGV[2] ~= ''
    |
    |if not isBot then
    |  for i, p in ipairs(lobby.players) do
    |    if p.name == inputName and (p.difficulty == nil or p.difficulty == cjson.null) then
    |      if not p.isOnline then
    |        p.isOnline = true
    |        redis.call('SET', KEYS[1], cjson.encode(lobby), 'EX', 86400)
    |        return cjson.encode(p)
    |      else
    |        return nil
    |      end
    |    end
    |  end
    |end
    |
    |if #lobby.players >= 6 then return nil end
    |
    |local newId = #lobby.players
    |local newPlayer = { id = newId, name = inputName }
    |if not isBot then
    |  newPlayer.difficulty = cjson.null
    |  newPlayer.isOnline = false
    |else
    |  newPlayer.difficulty = ARGV[2]
    |  newPlayer.name = 'Bot-' .. (newId+1)
    |  newPlayer.isOnline = true
    |end
    |
    |table.insert(lobby.players, newPlayer)
    |redis.call('SET', KEYS[1], cjson.encode(lobby), 'EX', 86400)
    |
    |return cjson.encode(newPlayer)
    |""".stripMargin

  /** @inheritdoc */
  override def addPlayer(
      lobbyId: LobbyId,
      name: String,
      difficulty: Option[BotsDifficulty]
  ): Future[Option[Player]] =
    val req = Request
      .cmd(Command.EVAL)
      .arg(addPlayerScript)
      .arg("1")
      .arg(ChannelsKeys.lobby(lobbyId))
      .arg(name)
      .arg(difficulty.map(_.toString).getOrElse(""))
      .arg(lobbyId.toString)

    redisClient
      .send(req)
      .asScala
      .map:
        case null     => None
        case response => response.toString.decodeAs[Player].toOption
      .flatMap:
        case Some(player) =>
          val msg = s"""{"type":"system","playerId":${player.id.toInt},"action":"joined"}"""
          val pubReq =
            Request.cmd(Command.PUBLISH).arg(ChannelsKeys.pubSubLobbyChannel(lobbyId)).arg(msg)
          redisClient.send(pubReq).asScala.map(_ => Some(player))
        case None => Future.successful(None)

  /** @inheritdoc */
  override def removePlayer(lobbyId: LobbyId, playerId: PlayerId): Future[Boolean] =
    getLobby(lobbyId).flatMap:
      case Some(lobby) =>
        val newPlayers = lobby.players.filterNot(_.id == playerId)
        if newPlayers.size == lobby.players.size then Future.successful(false)
        else
          val updateFuture =
            if newPlayers.isEmpty then
              val req = Request.cmd(Command.DEL).arg(ChannelsKeys.lobby(lobbyId))
              redisClient.send(req).asScala.as(true)
            else saveLobby(lobby.copy(players = newPlayers)).as(true)

          updateFuture.flatMap: success =>
            if success then
              val msg = s"""{"type":"system","playerId":${playerId.toInt},"action":"left"}"""
              val pubReq =
                Request.cmd(Command.PUBLISH).arg(ChannelsKeys.pubSubLobbyChannel(lobbyId)).arg(msg)
              redisClient.send(pubReq).asScala.as(true)
            else Future.successful(false)
      case None => Future.successful(false)

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

  private val setPlayerOnlineScript =
    """
      |local lobbyStr = redis.call('GET', KEYS[1])
      |if not lobbyStr then return 0 end
      |local lobby = cjson.decode(lobbyStr)
      |local targetPlayerId = tonumber(ARGV[1])
      |local isOnline = ARGV[2] == 'true'
      |local found = false
      |for i, player in ipairs(lobby.players) do
      |  if player.id == targetPlayerId then
      |    player.isOnline = isOnline
      |    found = true
      |    break
      |  end
      |end
      |if found then
      |  redis.call('SET', KEYS[1], cjson.encode(lobby), 'EX', 86400)
      |  return 1
      |else
      |  return 0
      |end
      |""".stripMargin

  /** @inheritdoc */
  override def setPlayerOnlineStatus(
      lobbyId: LobbyId,
      playerId: PlayerId,
      isOnline: Boolean
  ): Future[Boolean] =
    val req = Request
      .cmd(Command.EVAL)
      .arg(setPlayerOnlineScript)
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
