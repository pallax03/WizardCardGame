package it.unibo.pps.wizard.engine.adapters.redis

import it.unibo.pps.wizard.engine.lobby.LobbyId

import it.unibo.pps.wizard.engine.model.basic.PlayerId

object RedisKeys:
  def lobby(id: LobbyId): String = s"lobby:${id.toString}"
  def game(id: LobbyId): String = s"game:${id.toString}"
  
  def pubLobbyChannel(id: LobbyId): String = s"channel:${id.toString}"
  val subLobbyChannel: String = s"channel:lobbyId"
  def pubLobbyPlayerChannel(id: LobbyId, playerId: PlayerId): String = s"channel:${id.toString}:${playerId.toInt}"
  val subLobbyPlayerChannel: String = s"channel:lobbyId:playerId"
