package it.unibo.pps.wizard.application.web.http.endpoints

import io.circe.generic.auto.*
import it.unibo.pps.wizard.application.web.http.*
import it.unibo.pps.wizard.engine.lobby.{BotsDifficulty, LobbyId, Player}
import it.unibo.pps.wizard.engine.model.basic.PlayerId
import sttp.model.StatusCode
import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*
import it.unibo.pps.wizard.codecs.engine.lobby.LobbyCodecs.given
import it.unibo.pps.wizard.codecs.engine.model.basic.PlayerIdCodecs.given

case class ErrorResponse(message: String, code: String)
case class CreateLobbyRequest(name: String, bot: Option[BotsDifficulty])
case class LobbyCreatedResponse(lobbyId: LobbyId, playerId: PlayerId)
case class JoinLobbyRequest(name: String, bot: Option[BotsDifficulty])
case class LobbyStateResponse(lobbyId: LobbyId, players: List[Player])
case class GameStartedResponse(message: String)

given Schema[PlayerId] = Schema.string
given Schema[LobbyId] = Schema.string
given Schema[Player] = Schema.derived

object LobbyEndpoints:

  val baseEndpoint = endpoint
    .in("lobby")
    .errorOut(jsonBody[ErrorResponse])

  val createLobby = baseEndpoint.post
    .in(jsonBody[CreateLobbyRequest])
    .out(jsonBody[LobbyCreatedResponse])

  val joinLobby = baseEndpoint.post
    .in(path[String]("lobbyId")) 
    .in(jsonBody[JoinLobbyRequest])
    .out(jsonBody[LobbyStateResponse])

  val getLobbyInfo = baseEndpoint.get
    .in(path[String]("lobbyId"))
    .out(jsonBody[LobbyStateResponse])

  val startGame = baseEndpoint.post
    .in(path[String]("lobbyId") / "start")
    .out(jsonBody[GameStartedResponse])

  val removePlayer = baseEndpoint.delete
    .in(path[String]("lobbyId") / "player" / path[String]("playerId"))
    .out(statusCode(StatusCode.NoContent))
