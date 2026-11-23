package io.github.pallax03.wizard.application.web.http.endpoints

import io.github.pallax03.wizard.application.web.ApiSupport
import io.github.pallax03.wizard.application.web.http.*
import io.github.pallax03.wizard.codecs.engine.lobby.LobbyCodecs.given
import io.github.pallax03.wizard.codecs.engine.model.basic.PlayerIdCodecs.given
import io.github.pallax03.wizard.codecs.engine.model.core.state.GameStateCodecs.given
import io.github.pallax03.wizard.codecs.http.HttpCodecs.given
import io.github.pallax03.wizard.codecs.http.LobbyRequestCodecs.given
import io.github.pallax03.wizard.engine.lobby.{GameConfiguration, LobbyError, LobbyId}
import io.github.pallax03.wizard.engine.model.basic.PlayerId
import io.github.pallax03.wizard.engine.model.core.state.PlayerGameState

import sttp.model.StatusCode
import sttp.tapir.*
import sttp.tapir.json.circe.*

/** Tapir endpoint definitions for lobby room lifecycle, membership, and configuration. */
object LobbyEndpoints:

  /** Shared base for all lobby endpoints: prefix + tag + error mapping. */
  private val base: Endpoint[Unit, Unit, LobbyError, Unit, Any] =
    ApiSupport.baseLobbyEndpoint.tag("Lobby")

  /** POST - create a new lobby and return the creator's IDs. */
  val createLobby: Endpoint[Unit, JoinLobbyRequest, LobbyError, AuthLobbyPlayer, Any] =
    base.post
      .summary("Create lobby")
      .description(
        "Creates a new lobby with the given player name. Fails with 400 if lobby is full."
      )
      .in(jsonBody[JoinLobbyRequest])
      .out(jsonBody[AuthLobbyPlayer])

  /** POST /{lobbyId} - join an existing lobby. */
  val joinLobby: Endpoint[Unit, (LobbyId, JoinLobbyRequest), LobbyError, AuthLobbyPlayer, Any] =
    base.post
      .summary("Join lobby")
      .description("Adds a player (or bot) to an existing lobby identified by lobbyId.")
      .in(ApiSupport.lobbyIdPath)
      .in(jsonBody[JoinLobbyRequest])
      .out(jsonBody[AuthLobbyPlayer])

  /** GET /{lobbyId} - retrieve lobby state. */
  val getLobbyInfo: Endpoint[Unit, LobbyId, LobbyError, LobbyStateResponse, Any] =
    base.get
      .summary("Get lobby info")
      .description("Returns lobbyId and current players. 404 if lobby does not exist.")
      .in(ApiSupport.lobbyIdPath)
      .out(jsonBody[LobbyStateResponse])

  /** Shared secure base for lobby endpoints: requires Bearer token + lobbyId. */
  private val secureBase: Endpoint[String, LobbyId, LobbyError, Unit, Any] =
    ApiSupport.secureLobbyEndpoint.tag("Lobby")

  /** GET /{lobbyId}/game - retrieve the player's specific game state. */
  val getPlayerGame: Endpoint[String, LobbyId, LobbyError, PlayerGameState, Any] =
    secureBase.get
      .summary("Get game state")
      .description("Returns the PlayerGameState tailored for the authenticated player.")
      .in("game")
      .out(jsonBody[PlayerGameState])

  /** POST /{lobbyId}/start - start the game for a waiting lobby. */
  val startGame: Endpoint[String, LobbyId, LobbyError, Unit, Any] =
    secureBase.post
      .summary("Start game")
      .description(
        "Transitions a WAITING or PAUSED lobby to IN_GAME and triggers engine initialization."
      )
      .in("start")

  /** POST /{lobbyId}/pause - pause an active game. */
  val pauseGame: Endpoint[String, LobbyId, LobbyError, Unit, Any] =
    secureBase.post
      .summary("Pause game")
      .description("Transitions an IN_GAME lobby to PAUSED.")
      .in("pause")

  /** POST /{lobbyId}/return - return to lobby (deletes game). */
  val returnLobby: Endpoint[String, LobbyId, LobbyError, Unit, Any] =
    secureBase.post
      .summary("Return to lobby")
      .description(
        "Transitions FINISHED or PAUSED lobby back to WAITING and deletes the game in Redis."
      )
      .in("return")

  /** POST /{lobbyId}/configuration - update the game configuration. */
  val updateConfiguration
      : Endpoint[String, (LobbyId, GameConfiguration), LobbyError, GameConfiguration, Any] =
    secureBase.post
      .summary("Update Game Configuration")
      .description("Updates the game configuration for a waiting or paused lobby.")
      .in("configuration")
      .in(jsonBody[GameConfiguration])
      .out(jsonBody[GameConfiguration])

  /** DELETE /{lobbyId} - remove a player. */
  val removePlayer: Endpoint[String, (LobbyId, PlayerId), LobbyError, Unit, Any] =
    secureBase.delete
      .summary("Remove player")
      .description("Removes playerId from lobbyId. Authenticated player must be in the lobby.")
      .in(jsonBody[PlayerId])
      .out(statusCode(StatusCode.NoContent))
