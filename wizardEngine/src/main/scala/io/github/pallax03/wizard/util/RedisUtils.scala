package io.github.pallax03.wizard.util

import io.github.pallax03.wizard.engine.lobby.LobbyId
import io.github.pallax03.wizard.engine.model.basic.PlayerId
import io.vertx.redis.client.{Command, Request}

object RedisUtil:
  val DEFAULT_TTL: String = "86400"
  
  def setWithDefaultTTL(key: String, value: String, ttl: String = DEFAULT_TTL): Request =
    Request.cmd(Command.SET).arg(key).arg(value).arg("EX").arg(ttl)

object ChannelsKeys:
  /** Redis Stream where the engine publishes bot tasks (one entry per InvitationEvent for a bot). */
  val BOT_TASKS_STREAM: String = "bot:tasks"

  /** Consumer group name used by all BotManagerVerticle instances to compete for tasks. */
  val BOT_CONSUMER_GROUP: String = "bot_workers"

  /** Key that stores lobbies. */
  def lobby(id: LobbyId): String = s"lobby:${id.toString}"

  /** Key that stores game relative to a lobby (1:1). */
  def game(id: LobbyId): String = s"game:${id.toString}"

  /** Key that stores game checkpoints relative to a game. */
  def gameCheckpoint(id: LobbyId): String = s"${game(id)}:checkpoint"

  /** Routing Key for lobby and private player's lobby channel. */
  def pubSubLobbyChannel(id: LobbyId): String = s"channel:${id.toString}"
  def pubSubLobbyPlayerChannel(id: LobbyId, playerId: PlayerId): String =
    s"channel:${id.toString}:${playerId.toInt}"

  /** Key that stores every log. */
  val LOGS_CHANNEL: String = "system:logs"

  /** Key that stores a pending turn timer for a player. Expires after config.timer + grace seconds. */
  def turnTimer(lobbyId: LobbyId, playerId: PlayerId): String =
    s"timer:${lobbyId.toString}:${playerId.toInt}"

  def disconnectTimer(lobbyId: LobbyId): String =
    s"disconnect:${lobbyId.toString}"

  /** Key that stores the consecutive AFK strikes for a player. */

  /** Redis Pub/Sub channel for expired-key notifications (keyspace events). */
  val TURN_TIMER_KEYSPACE: String = "__keyevent@0__:expired"

  /** Channel to notify TurnTimerVerticle of a new turn. */
  val TURN_EVENTS_CHANNEL: String = "system:turn_events"
