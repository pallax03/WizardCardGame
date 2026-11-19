package io.github.pallax03.wizard.engine.lobby

import io.github.pallax03.wizard.engine.model.basic.PlayerId

object LobbyTestDSL:
  val defaultLobbyId: LobbyId = LobbyId("lobby-test")

  def human(id: PlayerId, secret: String = "default-secret"): Player =
    Player.human(id, s"Player-${id.toInt}", Some(secret)).copy(isOnline = true)

  def bot(id: PlayerId, difficulty: BotsDifficulty = BotsDifficulty.Dumb): Player =
    Player.bot(id, difficulty)

  extension (player: Player)
    infix def -(other: Player): List[Player] = List(player, other)
    def offline: Player = player.copy(isOnline = false)
    def online: Player = player.copy(isOnline = true)
    def withStrikes(n: Int): Player = player.copy(strikes = n)

  extension (players: List[Player])
    infix def -(other: Player): List[Player] = players :+ other
    def asLobby(status: LobbyStatus = LobbyStatus.WAITING): Lobby =
      Lobby(defaultLobbyId, players, status, GameConfiguration())

  def emptyLobby: Lobby = List.empty[Player].asLobby()

export LobbyTestDSL.*
