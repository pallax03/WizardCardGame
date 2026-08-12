package it.unibo.pps.wizard.application.web.http.routes

import io.circe.Json
import io.circe.syntax.*
import io.vertx.ext.web.{Router, RoutingContext}
import it.unibo.pps.wizard.codecs.engine.lobby.LobbyCodecs.given
import it.unibo.pps.wizard.engine.lobby.{Lobby, LobbyId, LobbyPlayer, LobbyStatus}
import it.unibo.pps.wizard.engine.model.basic.PlayerId
import it.unibo.pps.wizard.engine.ports.LobbyStatePort

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}

class LobbyRoutes(lobbyStatePort: LobbyStatePort):

  def mount(router: Router): Unit =
    router.post("/api/lobby").handler(handleLobbyCreation)
    router.get("/api/lobby/").handler(handleMissingLobby)
    router.get("/api/lobby/:uuid").handler(handleLobbyInfo)
    router.post("/api/lobby/:uuid/join").handler(handlePlayerJoin)

  private def handleLobbyCreation(ctx: RoutingContext): Unit =
    val lobbyId = LobbyId.generate
    val lobby = Lobby(lobbyId, List(player(PlayerId(0))), LobbyStatus.WAITING)
    lobbyStatePort.saveLobby(lobby).onComplete:
      case Success(_) =>
        respondJson(
          ctx,
          201,
          Json.obj("lobbyId" -> lobbyId.asJson, "playerId" -> 0.asJson)
        )
      case Failure(exception) => ctx.fail(500, exception)

  private def handleMissingLobby(ctx: RoutingContext): Unit =
    respondJson(ctx, 400, Json.obj("error" -> "Missing lobby id".asJson))

  private def handleLobbyInfo(ctx: RoutingContext): Unit =
    val uuid = ctx.pathParam("uuid")
    lobbyStatePort.getLobby(uuid).onComplete:
      case Success(Some(lobby)) => respondJson(ctx, 200, lobby.asJson)
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
              respondJson(ctx, 200, Json.obj("playerId" -> playerId.toInt.asJson))
            case Failure(exception) => ctx.fail(500, exception)
      case Success(None)      => respondJson(ctx, 404, notFound(uuid))
      case Failure(exception) => ctx.fail(500, exception)

  private def player(id: PlayerId): LobbyPlayer =
    LobbyPlayer(id, s"Player-${id.toInt}", None)

  private def notFound(uuid: String): Json =
    Json.obj("error" -> s"Lobby $uuid not found".asJson)

  def respondJson(ctx: RoutingContext, status: Int, body: Json): Unit =
    ctx
      .response()
      .setStatusCode(status)
      .putHeader("Content-Type", "application/json")
      .end(body.noSpaces)