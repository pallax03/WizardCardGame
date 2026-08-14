package it.unibo.pps.wizard.application.web.http.routes

import io.circe.Json, io.circe.syntax._
import io.circe.generic.auto.*
import io.vertx.ext.web.{Router, RoutingContext}

import it.unibo.pps.wizard.application.web.*
import it.unibo.pps.wizard.codecs.syntax.CodecSyntax.*

import it.unibo.pps.wizard.codecs.engine.lobby.LobbyCodecs.given
import it.unibo.pps.wizard.codecs.engine.model.basic.PlayerIdCodecs.given
import it.unibo.pps.wizard.engine.configuration.GameConfiguration
import it.unibo.pps.wizard.engine.lobby._
import it.unibo.pps.wizard.engine.ports.InboundPort
import it.unibo.pps.wizard.engine.ports.LobbyStatePort
import it.unibo.pps.wizard.util.FutureSyntax.onVertxComplete

import scala.util.Failure
import scala.util.Success

class LobbyRoutes(lobbyStatePort: LobbyStatePort, gameEngine: InboundPort):

  def mount(router: Router): Unit =
    router.post("/api/lobby").handler(handleLobby)
    router.post("/api/lobby/:lobbyId").handler(handleLobby)
    router.get("/api/lobby/:lobbyId").handler(handleLobbyInfo)
//    router.delete("/api/lobby/:lobbyId/player/:playerId").handler(handleRemovePlayer) // todo: no host check?
    router.post("/api/lobby/:lobbyId/start").handler(handleStartGame)
    router
      .get("/api/lobby/")
      .handler(
        handleMissingLobby
      ) // todo: it's really needed? just do a refactor and put in a general Router...

  private def handleLobby(ctx: RoutingContext): Unit =
    case class AddPlayerPayload(name: String, bot: Option[BotsDifficulty])
    val body = ctx.body().asString().decodeAs[AddPlayerPayload]
    val name: String = body.fold(_ => "Player", _.name)
    val bot: Option[BotsDifficulty] = body.fold(_ => None, _.bot)
    val lobbyId = ctx.request().extractLobbyId.getOrElse(LobbyId.generate)
    lobbyStatePort.addPlayer(lobbyId, name, bot).onVertxComplete(ctx):
      case Success(Some(player)) =>
        respondJson(ctx, 201, Json.obj("lobbyId" -> lobbyId.asJson, "playerId" -> player.id.asJson))
      case Success(None) => ctx.fail(401) //lobby is full
      case Failure(exception) => ctx.fail(500, exception)

  private def handleMissingLobby(ctx: RoutingContext): Unit =
    respondJson(ctx, 400, Json.obj("error" -> "Missing lobby id".asJson))

  private def handleLobbyInfo(ctx: RoutingContext): Unit =
    ctx.request().extractLobbyId match
      case Some(lobbyId) =>
        lobbyStatePort
          .getLobby(lobbyId)
          .onVertxComplete(ctx):
            case Success(Some(lobby)) => respondJson(ctx, 200, lobby.asJson)
            case Success(None)        => respondJson(ctx, 404, notFound(lobbyId.toString))
            case Failure(exception)   => ctx.fail(500, exception)
      case None => ???

  private def handleStartGame(ctx: RoutingContext): Unit =
    ctx.request().extractLobbyId match
      case Some(lobbyId) =>
        lobbyStatePort
          .getLobby(lobbyId)
          .onVertxComplete(ctx):
            case Success(Some(lobby)) =>
              if lobby.status == LobbyStatus.WAITING then
                lobbyStatePort
                  .saveLobby(lobby.copy(status = LobbyStatus.IN_GAME))
                  .onVertxComplete(ctx):
                    case Success(_) =>
                      gameEngine
                        .startGame(
                          lobbyId,
                          lobby.players.map(_.id),
                          GameConfiguration(1000, lobby.players)
                        )
                        .onVertxComplete(ctx):
                          case Success(_) =>
                            respondJson(ctx, 200, Json.obj("message" -> "Game started".asJson))
                            println(s"Game started for lobby $lobby")
                          case Failure(exception) =>
                            println(
                              s"Failed to start game for lobby $lobbyId: ${exception.getMessage}"
                            )
                    case Failure(exception) => ctx.fail(500, exception)
              else
                respondJson(
                  ctx,
                  400,
                  Json.obj("error" -> "Game already started or finished".asJson)
                )
            case Success(None)      => respondJson(ctx, 404, notFound(lobbyId.toString))
            case Failure(exception) => ctx.fail(500, exception)
      case None => ???

  private def notFound(uuid: String): Json =
    Json.obj("error" -> s"Lobby $uuid not found".asJson)

  def respondJson(ctx: RoutingContext, status: Int, message: Json): Unit =
    ctx
      .response()
      .setStatusCode(status)
      .putHeader("Content-Type", "application/json")
      .end(message.noSpaces)
