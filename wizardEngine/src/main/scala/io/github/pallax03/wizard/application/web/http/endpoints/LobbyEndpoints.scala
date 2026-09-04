package io.github.pallax03.wizard.application.web.http.endpoints

import io.github.pallax03.wizard.application.web.http.{HttpSupport, LobbyPlayer}
import io.github.pallax03.wizard.codecs.engine.model.core.state.GameStateCodecs.given
import io.github.pallax03.wizard.codecs.http.HttpCodecs.given
import io.github.pallax03.wizard.codecs.http.LobbyRequestCodecs.given
import io.github.pallax03.wizard.engine.errors.AppError
import io.github.pallax03.wizard.engine.lobby.{BotsDifficulty, LobbyId}
import io.github.pallax03.wizard.engine.model.basic.PlayerId
import io.github.pallax03.wizard.engine.model.core.state.PlayerGameState

import sttp.model.StatusCode
import sttp.tapir.*
import sttp.tapir.json.circe.*

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

object LobbyEndpoints:

  /** Shared base for all lobby endpoints: prefix + tag + error mapping. */
  private val base: Endpoint[Unit, Unit, AppError, Unit, Any] =
    endpoint
      .in("api" / "lobby")
      .tag("Lobby")
      .errorOut(HttpSupport.errorOutput)

  /** POST /api/lobby — create a new lobby and return the creator's IDs. */
  val createLobby: Endpoint[Unit, JoinLobbyRequest, AppError, LobbyPlayer, Any] =
    base.post
      .summary("Create lobby")
      .description(
        "Creates a new lobby with the given player name. Fails with 400 if lobby is full."
      )
      .in(jsonBody[JoinLobbyRequest])
      .out(jsonBody[LobbyPlayer])

  /** POST /api/lobby/{lobbyId} — join an existing lobby. */
  val joinLobby: Endpoint[Unit, (LobbyId, JoinLobbyRequest), AppError, LobbyPlayer, Any] =
    base.post
      .summary("Join lobby")
      .description("Adds a player (or bot) to an existing lobby identified by lobbyId.")
      .in(HttpSupport.lobbyIdPath)
      .in(jsonBody[JoinLobbyRequest])
      .out(jsonBody[LobbyPlayer])

  /** GET /api/lobby/{lobbyId} — retrieve lobby state. */
  val getLobbyInfo: Endpoint[Unit, LobbyId, AppError, LobbyStateResponse, Any] =
    base.get
      .summary("Get lobby info")
      .description("Returns lobbyId and current players. 404 if lobby does not exist.")
      .in(HttpSupport.lobbyIdPath)
      .out(jsonBody[LobbyStateResponse])

  /** Shared secure base for lobby endpoints: requires Bearer token. */
  private val secureBase: Endpoint[String, Unit, AppError, Unit, Any] =
    base.securityIn(auth.bearer[String]())

  /** GET /api/lobby/{lobbyId}/game — retrieve the player's specific game state. */
  val getPlayerGame: Endpoint[String, LobbyId, AppError, PlayerGameState, Any] =
    secureBase.get
      .summary("Get game state")
      .description("Returns the PlayerGameState tailored for the authenticated player.")
      .in(HttpSupport.lobbyIdPath / "game")
      .out(jsonBody[PlayerGameState])

  /** POST /api/lobby/{lobbyId}/start — start the game for a waiting lobby. */
  val startGame: Endpoint[String, LobbyId, AppError, GameStartedResponse, Any] =
    secureBase.post
      .summary("Start game")
      .description("Transitions a WAITING lobby to IN_GAME and triggers engine initialization.")
      .in(HttpSupport.lobbyIdPath / "start")
      .out(jsonBody[GameStartedResponse])

  /** DELETE /api/lobby — remove a player (body-based for backward compat with frontend). */
  val removePlayer: Endpoint[String, LobbyPlayer, AppError, Unit, Any] =
    secureBase.delete
      .summary("Remove player")
      .description("Removes playerId from lobbyId. Authenticated player must be in the lobby.")
      .in(jsonBody[LobbyPlayer])
      .out(statusCode(StatusCode.NoContent))
