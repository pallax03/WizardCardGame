package it.unibo.pps.wizard.application.web.http.routes

import io.circe.Json
import io.vertx.ext.web.{Router, RoutingContext}
import it.unibo.pps.wizard.codecs.engine.lobby.LobbyCodecs.given
import it.unibo.pps.wizard.codecs.syntax.CodecSyntax.*
import it.unibo.pps.wizard.engine.configuration.GameConfiguration
import it.unibo.pps.wizard.engine.lobby.{Lobby, LobbyId, LobbyPlayer, LobbyStatus}
import it.unibo.pps.wizard.engine.model.basic.PlayerId
import it.unibo.pps.wizard.engine.ports.{GameEngineInboundPort, LobbyStatePort}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}

class LobbyRoutes(lobbyStatePort: LobbyStatePort, gameEngine: GameEngineInboundPort):

  def mount(router: Router): Unit =
    router.post("/api/lobby").handler(handleLobbyCreation)
    router.get("/api/lobby/").handler(handleMissingLobby)
    router.get("/api/lobby/:uuid").handler(handleLobbyInfo)
    router.post("/api/lobby/:uuid/join").handler(handlePlayerJoin)
    router.post("/api/lobby/:uuid/start").handler(handleStartGame)

  private def handleLobbyCreation(ctx: RoutingContext): Unit =
    val lobbyId = LobbyId.generate
    val lobby = Lobby(lobbyId, List(player(PlayerId(0))), LobbyStatus.WAITING)
    lobbyStatePort.saveLobby(lobby).onComplete:
      case Success(_) =>
        respondJson(
          ctx,
          201,
          Json.obj("lobbyId" -> lobbyId.toJson, "playerId" -> 0.toJson)
        )
      case Failure(exception) => ctx.fail(500, exception)

  private def handleMissingLobby(ctx: RoutingContext): Unit =
    respondJson(ctx, 400, Json.obj("error" -> "Missing lobby id".toJson))

  private def handleLobbyInfo(ctx: RoutingContext): Unit =
    val uuid = ctx.pathParam("uuid")
    lobbyStatePort.getLobby(uuid).onComplete:
      case Success(Some(lobby)) => respondJson(ctx, 200, lobby.toJson)
      case Success(None)        => respondJson(ctx, 404, notFound(uuid))
      case Failure(exception)   => ctx.fail(500, exception)

  private def handlePlayerJoin(ctx: RoutingContext): Unit =
    val uuid = ctx.pathParam("uuid")
    lobbyStatePort.getLobby(uuid).onComplete:
      case Success(Some(lobby)) =>
        val playerId = PlayerId(lobby.players.size)
        lobbyStatePort
          .saveLobby(lobby.copy(players = lobby.players :+ player(playerId)))
          .onComplete:
            case Success(_) =>
              respondJson(ctx, 200, Json.obj("playerId" -> playerId.toInt.toJson))
            case Failure(exception) => ctx.fail(500, exception)
      case Success(None)      => respondJson(ctx, 404, notFound(uuid))
      case Failure(exception) => ctx.fail(500, exception)

  private def handleStartGame(ctx: RoutingContext): Unit =
    val uuid = ctx.pathParam("uuid")
    lobbyStatePort.getLobby(uuid).onComplete:
      case Success(Some(lobby)) =>
        if lobby.status == LobbyStatus.WAITING then
          lobbyStatePort
            .saveLobby(lobby.copy(status = LobbyStatus.IN_GAME))
            .onComplete:
              case Success(_) =>
                gameEngine
                  .startGame(lobby.players.map(_.id), GameConfiguration(1000, lobby.players))
                  .onComplete:
                    case Success(_) =>
                      respondJson(ctx, 200, Json.obj("message" -> "Game started".toJson))
                      println(s"Game started for lobby $uuid")
                    case Failure(exception) => println(s"Failed to start game for lobby $uuid: ${exception.getMessage}")
              case Failure(exception) => ctx.fail(500, exception)
        else
          respondJson(ctx, 400, Json.obj("error" -> "Game already started or finished".toJson))
      case Success(None)      => respondJson(ctx, 404, notFound(uuid))
      case Failure(exception) => ctx.fail(500, exception)

  private def player(id: PlayerId): LobbyPlayer =
    LobbyPlayer(id, s"Player-${id.toInt}", None)

  private def notFound(uuid: String): Json =
    Json.obj("error" -> s"Lobby $uuid not found".toJson)

  def respondJson(ctx: RoutingContext, status: Int, message: Json): Unit =
//    val body = status match
//      case 200 | 201 => ???
//      case _         => ???
    
    ctx
      .response()
      .setStatusCode(status)
      .putHeader("Content-Type", "application/json")
      .end(message.noSpaces)