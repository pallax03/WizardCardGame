package it.unibo.pps.wizard.application.web.http.routes;

import io.circe.Json
import io.vertx.ext.web.{Router, RoutingContext}
import it.unibo.pps.wizard.engine.ports.{GameEngineInboundPort, LobbyStatePort}
import it.unibo.pps.wizard.codecs.syntax.CodecSyntax.*
import it.unibo.pps.wizard.engine.model.core.GameAction
import it.unibo.pps.wizard.codecs.engine.model.core.GameActionCodecs.given

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}

class ActionRoutes(lobbyStatePort: LobbyStatePort, gameEnginePort: GameEngineInboundPort):
  
  def mount(router: Router): Unit =
    router.post("/api/lobby/:uuid/player/:id/choose").handler(handleSubmitAction)
    router.post("/api/lobby/:uuid/player/:id/place").handler(handleSubmitAction)
    router.post("/api/lobby/:uuid/player/:id/play").handler(handleSubmitAction)
    
  private def handleSubmitAction(ctx: RoutingContext): Unit =
    val uuid = ctx.pathParam("uuid")
    val playerId = ctx.pathParam("id")
    val rawJson = ctx.body().asString()

    rawJson.decodeAs[GameAction] match
      case Left(error) => respondJson(ctx, 400, Json.obj("error" -> s"Invalid JSON body: ${error.getMessage}".toJson))
      case Right(action) =>
        lobbyStatePort.getLobby(uuid).onComplete:
          case Success(Some(lobby)) =>
            gameEnginePort.submitAction(action)
            respondJson(ctx, 200, Json.obj("message" -> "Action submitted successfully".toJson))
            println(s"$playerId, $gameEnginePort, $lobby")
          case Success(None)        => respondJson(ctx, 404, notFound(uuid))
          case Failure(exception)   => ctx.fail(500, exception)

  private def notFound(uuid: String): Json =
    Json.obj("error" -> s"Lobby $uuid not found".toJson)

  def respondJson(ctx: RoutingContext, status: Int, body: Json): Unit =
    ctx
      .response()
      .setStatusCode(status)
      .putHeader("Content-Type", "application/json")
      .end(body.noSpaces)
