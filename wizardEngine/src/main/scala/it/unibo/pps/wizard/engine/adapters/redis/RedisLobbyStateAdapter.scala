package it.unibo.pps.wizard.engine.adapters.redis

import io.vertx.redis.client.{Command, Redis, Request}
import it.unibo.pps.wizard.codecs.engine.lobby.LobbyCodecs.given
import it.unibo.pps.wizard.codecs.syntax.CodecSyntax.*
import it.unibo.pps.wizard.engine.lobby.*
import it.unibo.pps.wizard.engine.model.basic.PlayerId
import it.unibo.pps.wizard.engine.ports.LobbyStatePort

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import it.unibo.pps.wizard.util.FutureSyntax.*

class RedisLobbyStateAdapter(redisClient: Redis) extends LobbyStatePort:


  override def saveLobby(lobby: Lobby): Future[Unit] =
    val req = Request.cmd(Command.SET).arg(RedisKeys.lobby(lobby.uuid)).arg(lobby.toJson)
    redisClient.send(req).asScala.map(_ => ())

  override def getLobby(lobbyId: LobbyId): Future[Option[Lobby]] =
    val req = Request.cmd(Command.GET).arg(RedisKeys.lobby(lobbyId))
    redisClient.send(req).asScala.map:
      case null => None
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
      |  newPlayer.bot = cjson.null
      |else
      |  newPlayer.bot = ARGV[2]
      |end
      |
      |table.insert(lobby.players, newPlayer)
      |redis.call('SET', KEYS[1], cjson.encode(lobby))
      |
      |return cjson.encode(newPlayer)
      |""".stripMargin

  override def addPlayer(lobbyId: LobbyId, name: String, bot: Option[BotsDifficulty]): Future[Option[Player]] =
    val botArg = bot.map(_.toString).getOrElse("")
    val req = Request.cmd(Command.EVAL)
      .arg(addPlayerScript).arg("1").arg(RedisKeys.lobby(lobbyId))
      .arg(name).arg(botArg).arg(lobbyId.toString)
      
    redisClient.send(req).asScala.map:
      case null => None
      case response => response.toString.decodeAs[Player].toOption

  override def removePlayer(lobbyId: LobbyId, playerId: PlayerId): Future[Boolean] =
    getLobby(lobbyId).flatMap:
      case Some(lobby) =>
        val newPlayers = lobby.players.filterNot(_.id == playerId)
        if newPlayers.size == lobby.players.size then Future.successful(false)
        else saveLobby(lobby.copy(players = newPlayers)).map(_ => true)
      case None => Future.successful(false)
