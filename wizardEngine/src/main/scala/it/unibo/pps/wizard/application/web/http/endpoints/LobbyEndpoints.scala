package it.unibo.pps.wizard.application.web.http.endpoints

import io.circe.generic.auto._
import it.unibo.pps.wizard.application.web.http._
import it.unibo.pps.wizard.codecs.engine.lobby.LobbyCodecs.given
import it.unibo.pps.wizard.codecs.engine.model.basic.PlayerIdCodecs.given
import it.unibo.pps.wizard.engine.lobby.BotsDifficulty
import it.unibo.pps.wizard.engine.lobby.LobbyId
import it.unibo.pps.wizard.engine.lobby.Player
import it.unibo.pps.wizard.engine.model.basic.PlayerId
import sttp.model.StatusCode
import sttp.tapir._
import sttp.tapir.generic.auto._
import sttp.tapir.json.circe._

case class ErrorResponse(message: String, code: String)
case class JoinLobbyRequest(name: String, bot: Option[BotsDifficulty])
case class LobbyPlayerResponse(lobbyId: LobbyId, playerId: PlayerId)
case class LobbyStateResponse(lobbyId: LobbyId, players: List[Player])
case class GameStartedResponse(message: String)

given Schema[PlayerId] = Schema.string
given Schema[LobbyId] = Schema.string
given Schema[Player] = Schema.derived

object LobbyEndpoints:

  val baseEndpoint: Endpoint[Unit, Unit, ErrorResponse, Unit, Any] = endpoint
    .in("api")
    .in("lobby")
    .errorOut(jsonBody[ErrorResponse])

  val createLobby: Endpoint[Unit, JoinLobbyRequest, ErrorResponse, LobbyPlayerResponse, Any] =
    baseEndpoint.post
      .in(jsonBody[JoinLobbyRequest])
      .out(jsonBody[LobbyPlayerResponse])

  val joinLobby: Endpoint[Unit, (String, JoinLobbyRequest), ErrorResponse, LobbyPlayerResponse, Any] =
    baseEndpoint.post
      .in(path[String]("lobbyId"))
      .in(jsonBody[JoinLobbyRequest])
      .out(jsonBody[LobbyPlayerResponse])

  val getLobbyInfo: Endpoint[Unit, String, ErrorResponse, LobbyStateResponse, Any] =
    baseEndpoint.get
      .in(path[String]("lobbyId"))
      .out(jsonBody[LobbyStateResponse])

  val startGame: Endpoint[Unit, String, ErrorResponse, GameStartedResponse, Any] = baseEndpoint.post
    .in(path[String]("lobbyId") / "start")
    .out(jsonBody[GameStartedResponse])

  val removePlayer: Endpoint[Unit, LobbyPlayerResponse, ErrorResponse, Unit, Any] = baseEndpoint.delete
    .in(jsonBody[LobbyPlayerResponse])
    .out(statusCode(StatusCode.NoContent))
