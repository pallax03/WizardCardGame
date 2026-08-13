package it.unibo.pps.wizard.engine.adapters.redis

import it.unibo.pps.wizard.engine.lobby.LobbyId

import it.unibo.pps.wizard.engine.model.basic.PlayerId

object RedisKeys:
  def lobby(id: LobbyId): String = s"lobby:${id.toString}"
  def game(id: LobbyId): String = s"game:${id.toString}"
  def pubSubChannel(id: LobbyId): String = s"channel:${id.toString}"
  def pubSubPlayerChannel(id: LobbyId, playerId: PlayerId): String = s"channel:${id.toString}:${playerId.toInt}"
