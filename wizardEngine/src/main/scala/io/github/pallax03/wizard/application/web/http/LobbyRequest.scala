package io.github.pallax03.wizard.application.web.http

import io.github.pallax03.wizard.engine.lobby.{
  BotsDifficulty,
  GameConfiguration,
  LobbyId,
  LobbyStatus
}
import io.github.pallax03.wizard.engine.model.basic.PlayerId

/** Request payload to create or join a lobby as a human or automated bot. */
case class JoinLobbyRequest(
    name: String,
    difficulty: Option[BotsDifficulty],
    secret: Option[String] = None
)

/** Public player information sanitized of private authentication secrets. */
case class PublicPlayerInfo(
    id: PlayerId,
    name: String,
    difficulty: Option[BotsDifficulty] = None,
    isOnline: Boolean = false,
    strikes: Int = 0
)

/** Public snapshot of lobby session composition and lifecycle status. */
case class LobbyStateResponse(
    lobbyId: LobbyId,
    status: LobbyStatus,
    players: List[PublicPlayerInfo],
    configuration: GameConfiguration,
    createdAt: Long
)

/** Registration payload returned upon joining, containing the player's private session secret. */
case class AuthLobbyPlayer(lobbyId: LobbyId, playerId: PlayerId, secret: Option[String] = None)
