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
    val req = Request.cmd(Command.SET).arg(ChannelsKeys.lobby(lobby.uuid)).arg(lobby.toJson).arg("EX").arg("86400")
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
      |if #lobby.players >= 6 then return nil end
      |
      |local newId = #lobby.players
      |local newPlayer = { id = newId, name = ARGV[1] }
      |if ARGV[2] == '' then
      |  newPlayer.difficulty = cjson.null
      |else
      |  newPlayer.difficulty = ARGV[2]
      |  newPlayer.name = 'Bot-' .. (newId+1)
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

  /** @inheritdoc */
  override def removePlayer(lobbyId: LobbyId, playerId: PlayerId): Future[Boolean] =
    getLobby(lobbyId).flatMap:
      case Some(lobby) =>
        val newPlayers = lobby.players.filterNot(_.id == playerId)
        if newPlayers.size == lobby.players.size then Future.successful(false)
        else if newPlayers.isEmpty then
          val req = Request.cmd(Command.DEL).arg(ChannelsKeys.lobby(lobbyId))
          redisClient.send(req).asScala.as(true)
        else saveLobby(lobby.copy(players = newPlayers)).as(true)
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
            val mgetReq = Request.cmd(Command.MGET)
            keys.foreach(mgetReq.arg)
            redisClient
              .send(mgetReq)
              .asScala
              .map:
                case null => List.empty
                case valsResp =>
                  valsResp.asScala
                    .flatMap(v => if v != null then v.toString.decodeAs[Lobby].toOption else None)
                    .toList

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
