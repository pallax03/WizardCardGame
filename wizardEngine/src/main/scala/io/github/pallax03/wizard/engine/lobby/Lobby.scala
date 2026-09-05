package io.github.pallax03.wizard.engine.lobby

import io.github.pallax03.wizard.engine.configuration.GameConfiguration

import java.util.UUID

/** Represents the status of a Lobby. */
enum LobbyStatus:
  case WAITING, IN_GAME, PAUSED, FINISHED

  override def toString: String = super.toString.toUpperCase

opaque type LobbyId = String

object LobbyId:
  def apply(id: String): LobbyId = id
  def generate: LobbyId = UUID.randomUUID().toString

/**
 * Represents a game lobby in the application layer.
 *
 * @param uuid the globally unique identifier of the lobby.
 * @param players the list of players currently in the lobby.
 * @param status the current lifecycle status of the lobby.
 */
case class Lobby(
    uuid: LobbyId,
    players: List[Player],
    status: LobbyStatus,
    configuration: GameConfiguration
):
  def addPlayer(player: Player): Lobby = copy(players = this.players :+ player)
