package io.github.pallax03.wizard.util

import io.vertx.redis.client.{Command, Request}

import io.github.pallax03.wizard.engine.lobby.LobbyId
import io.github.pallax03.wizard.engine.model.basic.PlayerId

/** Redis utility constants and request formatting helpers. */
object RedisUtil:
  /** Default TTL in seconds for lobby and active game session state keys (24 hours). */
  val DEFAULT_TTL: String = "86400"

  def setWithDefaultTTL(key: String, value: String, ttl: String = DEFAULT_TTL): Request =
    Request.cmd(Command.SET).arg(key).arg(value).arg("EX").arg(ttl)

/** Central registry of Redis key namespaces, stream names, and Pub/Sub channel schemas. */
object ChannelsKeys:
  /** Redis Stream where the engine publishes bot tasks (one entry per [[io.github.pallax03.wizard.engine.model.events.InvitationEvent]]). */
  val BOT_TASKS_STREAM: String = "bot:tasks"

  /** Consumer group name used by competing [[io.github.pallax03.wizard.application.bot.BotManagerVerticle]] instances. */
  val BOT_CONSUMER_GROUP: String = "bot_workers"

  /** Key storing serialized lobby state: `lobby:<lobbyId>`. */
  def lobby(id: LobbyId): String = s"lobby:${id.toString}"

  /** Key storing active game state: `game:<lobbyId>`. */
  def game(id: LobbyId): String = s"game:${id.toString}"

  /** Key storing stable round recovery checkpoint: `game:<lobbyId>:checkpoint`. */
  def gameCheckpoint(id: LobbyId): String = s"${game(id)}:checkpoint"

  /** Pub/Sub channel broadcasting events to all clients in a lobby: `channel:<lobbyId>`. */
  def pubSubLobbyChannel(id: LobbyId): String = s"channel:${id.toString}"

  /** Pub/Sub channel routing private unicast events to a specific player: `channel:<lobbyId>:<playerId>`. */
  def pubSubLobbyPlayerChannel(id: LobbyId, playerId: PlayerId): String =
    s"channel:${id.toString}:${playerId.toInt}"

  /** Expiring key tracking turn deadlines: `timer:<lobbyId>:<playerId>`. */
  def turnTimer(lobbyId: LobbyId, playerId: PlayerId): String =
    s"timer:${lobbyId.toString}:${playerId.toInt}"

  /** Expiring key tracking disconnected lobby retention: `disconnect:<lobbyId>`. */
  def disconnectTimer(lobbyId: LobbyId): String =
    s"disconnect:${lobbyId.toString}"

  /** Redis keyspace notification topic monitored for expired turn timer keys. */
  val TURN_TIMER_KEYSPACE: String = "__keyevent@0__:expired"

  /** Internal Pub/Sub channel broadcasting turn initiation events to timer verticles. */
  val TURN_EVENTS_CHANNEL: String = "system:turn_events"
