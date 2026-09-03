package io.github.pallax03.wizard.application.web.http.endpoints

import io.circe.generic.auto.*

import io.github.pallax03.wizard.application.web.http.HttpSupport.given
import io.github.pallax03.wizard.application.web.http.{ErrorResponse, HttpSupport}
import io.github.pallax03.wizard.codecs.engine.lobby.LobbyCodecs.given
import io.github.pallax03.wizard.codecs.engine.model.basic.PlayerIdCodecs.given
import io.github.pallax03.wizard.engine.lobby.{BotsDifficulty, LobbyId, Player}
import io.github.pallax03.wizard.engine.model.basic.PlayerId

import sttp.model.StatusCode
import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*

case class JoinLobbyRequest(name: String, bot: Option[BotsDifficulty])
case class LobbyPlayerResponse(lobbyId: LobbyId, playerId: PlayerId)
case class LobbyStateResponse(lobbyId: LobbyId, players: List[Player])
case class GameStartedResponse(message: String)

object LobbyEndpoints:

  given Schema[Player] = Schema.derived
  given Schema[JoinLobbyRequest] = Schema.derived
  given Schema[LobbyPlayerResponse] = Schema.derived
  given Schema[LobbyStateResponse] = Schema.derived
  given Schema[GameStartedResponse] = Schema.derived

  /** Shared base for all lobby endpoints: prefix + tag + error mapping. */
  private val base: Endpoint[Unit, Unit, ErrorResponse, Unit, Any] =
    endpoint
      .in("api" / "lobby")
      .tag("Lobby")
      .errorOut(HttpSupport.errorOutput)

  /** POST /api/lobby — create a new lobby and return the creator's IDs. */
  val createLobby: Endpoint[Unit, JoinLobbyRequest, ErrorResponse, LobbyPlayerResponse, Any] =
    base.post
      .summary("Create lobby")
      .description(
        "Creates a new lobby with the given player name. Fails with 400 if lobby is full."
      )
      .in(jsonBody[JoinLobbyRequest])
      .out(jsonBody[LobbyPlayerResponse])

  /** POST /api/lobby/{lobbyId} — join an existing lobby. */
  val joinLobby
      : Endpoint[Unit, (LobbyId, JoinLobbyRequest), ErrorResponse, LobbyPlayerResponse, Any] =
    base.post
      .summary("Join lobby")
      .description("Adds a player (or bot) to an existing lobby identified by lobbyId.")
      .in(HttpSupport.lobbyIdPath)
      .in(jsonBody[JoinLobbyRequest])
      .out(jsonBody[LobbyPlayerResponse])

  /** GET /api/lobby/{lobbyId} — retrieve lobby state. */
  val getLobbyInfo: Endpoint[Unit, LobbyId, ErrorResponse, LobbyStateResponse, Any] =
    base.get
      .summary("Get lobby info")
      .description("Returns lobbyId and current players. 404 if lobby does not exist.")
      .in(HttpSupport.lobbyIdPath)
      .out(jsonBody[LobbyStateResponse])

  /** POST /api/lobby/{lobbyId}/start — start the game for a waiting lobby. */
  val startGame: Endpoint[Unit, LobbyId, ErrorResponse, GameStartedResponse, Any] =
    base.post
      .summary("Start game")
      .description("Transitions a WAITING lobby to IN_GAME and triggers engine initialization.")
      .in(HttpSupport.lobbyIdPath / "start")
      .out(jsonBody[GameStartedResponse])

  /** DELETE /api/lobby — remove a player (body-based for backward compat with frontend). */
  val removePlayer: Endpoint[Unit, LobbyPlayerResponse, ErrorResponse, Unit, Any] =
    base.delete
      .summary("Remove player")
      .description("Removes playerId from lobbyId. Returns 204 on success, 404 if not found.")
      .in(jsonBody[LobbyPlayerResponse])
      .out(statusCode(StatusCode.NoContent))
