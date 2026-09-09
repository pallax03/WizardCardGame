package io.github.pallax03.wizard.application.web.http

import io.github.pallax03.wizard.engine.lobby.{BotsDifficulty, GameConfiguration, LobbyId, LobbyStatus}
import io.github.pallax03.wizard.engine.model.basic.PlayerId

case class JoinLobbyRequest(
    name: String,
    difficulty: Option[BotsDifficulty],
    secret: Option[String] = None
)

case class PublicPlayerInfo(
    id: PlayerId,
    name: String,
    difficulty: Option[BotsDifficulty] = None,
    isOnline: Boolean = false
)

case class LobbyStateResponse(
    lobbyId: LobbyId,
    status: LobbyStatus,
    players: List[PublicPlayerInfo],
    configuration: GameConfiguration
)

case class AuthLobbyPlayer(lobbyId: LobbyId, playerId: PlayerId, secret: Option[String] = None)
