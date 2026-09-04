package io.github.pallax03.wizard.application.web.http

import io.github.pallax03.wizard.engine.lobby.{BotsDifficulty, LobbyId}
import io.github.pallax03.wizard.engine.model.basic.PlayerId

// --- Lobby Endpoints Payloads ---

case class JoinLobbyRequest(
    name: String,
    bot: Option[BotsDifficulty],
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
    players: List[PublicPlayerInfo]
)

case class GameStartedResponse(message: String)

// --- Shared & Auth Payloads ---

case class ActionSuccessResponse(message: String)

case class AuthLobbyPlayer(lobbyId: LobbyId, playerId: PlayerId, secret: Option[String] = None)

case class LobbyPlayer(lobbyId: LobbyId, playerId: PlayerId)
