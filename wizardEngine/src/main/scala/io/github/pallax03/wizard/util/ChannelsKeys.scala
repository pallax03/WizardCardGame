package io.github.pallax03.wizard.util

import io.github.pallax03.wizard.engine.lobby.LobbyId
import io.github.pallax03.wizard.engine.model.basic.PlayerId

object ChannelsKeys:
  val SPAWN_BOT_CHANNEL: String = "bots:spawn"
  def lobby(id: LobbyId): String = s"lobby:${id.toString}"
  val LOBBY_CHANNEL: String = "lobby:*"
  def game(id: LobbyId): String = s"game:${id.toString}"
  def gameCheckpoint(id: LobbyId): String = s"${game(id)}:checkpoint"
  val GAME_CHANNEL: String = "game:*"

  def pubSubLobbyChannel(id: LobbyId): String = s"channel:${id.toString}"
  def pubSubLobbyPlayerChannel(id: LobbyId, playerId: PlayerId): String =
    s"channel:${id.toString}:${playerId.toInt}"
  def botLock(id: LobbyId): String = s"lock:bot:${id.toString}"

  val LOGS_CHANNEL: String = "system:logs"
