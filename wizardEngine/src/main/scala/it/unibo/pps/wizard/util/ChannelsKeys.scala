package it.unibo.pps.wizard.util

import it.unibo.pps.wizard.engine.lobby.LobbyId
import it.unibo.pps.wizard.engine.model.basic.PlayerId

object ChannelsKeys:
  val SPAWN_BOT_CHANNEL: String = "bots:spawn"
  def lobby(id: LobbyId): String = s"lobby:${id.toString}"
  val LOBBY_CHANNEL: String = "lobby:*"
  def game(id: LobbyId): String = s"game:${id.toString}"
  val GAME_CHANNEL: String =  "game:*"
  
  def pubSubLobbyChannel(id: LobbyId): String = s"channel:${id.toString}"
  def pubSubLobbyPlayerChannel(id: LobbyId, playerId: PlayerId): String = s"channel:${id.toString}:${playerId.toInt}"
  def botLock(id: LobbyId): String = s"lock:bot:${id.toString}"
