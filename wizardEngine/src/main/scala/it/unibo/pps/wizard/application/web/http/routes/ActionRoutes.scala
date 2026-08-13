package it.unibo.pps.wizard.application.web.http.routes

import io.circe.Json
import io.circe.syntax.*
import io.vertx.ext.web.{Router, RoutingContext}
import it.unibo.pps.wizard.engine.ports.{InboundPort, LobbyStatePort}
import it.unibo.pps.wizard.codecs.syntax.CodecSyntax.*
import it.unibo.pps.wizard.engine.lobby.LobbyId
import it.unibo.pps.wizard.engine.model.core.GameAction
import it.unibo.pps.wizard.codecs.engine.model.core.GameActionCodecs.given

import it.unibo.pps.wizard.util.FutureSyntax.onVertxComplete
import scala.util.{Failure, Success}

class ActionRoutes(lobbyStatePort: LobbyStatePort, gameEnginePort: InboundPort):
  def mount(router: Router): Unit =
    router.post("/api/lobby/:lobbyId/player/:playerId/choose").handler(handleSubmitAction)
    router.post("/api/lobby/:lobbyId/player/:playerId/place").handler(handleSubmitAction)
    router.post("/api/lobby/:lobbyId/player/:playerId/play").handler(handleSubmitAction)
    
  private def handleSubmitAction(ctx: RoutingContext): Unit =
    val uuid = ctx.pathParam("lobbyId")
    val playerId = ctx.pathParam("playerId")
    val rawJson = ctx.body().asString()

    rawJson.decodeAs[GameAction] match
      case Left(error) => respondJson(ctx, 400, Json.obj("error" -> s"Invalid JSON body: ${error.getMessage}".asJson))
      case Right(action) =>
        lobbyStatePort.getLobby(LobbyId(uuid)).onVertxComplete(ctx):
          case Success(Some(lobby)) =>
            gameEnginePort.submitAction(LobbyId(uuid), action)
            respondJson(ctx, 200, Json.obj("message" -> "Action submitted successfully".asJson))
            println(s"$playerId, $gameEnginePort, $lobby")
          case Success(None)        => respondJson(ctx, 404, notFound(uuid))
          case Failure(exception)   => ctx.fail(500, exception)

  private def notFound(uuid: String): Json =
    Json.obj("error" -> s"Lobby $uuid not found".asJson)

  def respondJson(ctx: RoutingContext, status: Int, body: Json): Unit =
    ctx
      .response()
      .setStatusCode(status)
      .putHeader("Content-Type", "application/json")
      .end(body.noSpaces)
