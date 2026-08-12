package it.unibo.pps.wizard.application.web.http.routes

import io.circe.Json
import io.circe.syntax.*
import io.vertx.ext.web.{Router, RoutingContext}
import it.unibo.pps.wizard.codecs.engine.lobby.LobbyCodecs.given
import it.unibo.pps.wizard.engine.configuration.GameConfiguration
import it.unibo.pps.wizard.engine.lobby.*
import it.unibo.pps.wizard.engine.ports.{GameEngineInboundPort, LobbyStatePort}
import it.unibo.pps.wizard.application.web.*
import it.unibo.pps.wizard.codecs.engine.model.basic.PlayerIdCodecs.given 


import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}

class LobbyRoutes(lobbyStatePort: LobbyStatePort, gameEngine: GameEngineInboundPort):

  def mount(router: Router): Unit =
    router.post("/api/lobby").handler(handleLobby)
    router.get("/api/lobby/").handler(handleMissingLobby)
    router.get("/api/lobby/:lobbyId").handler(handleLobbyInfo)
    router.post("/api/lobby/:lobbyId/join").handler(handleLobby)
    router.post("/api/lobby/:lobbyId/start").handler(handleStartGame)
  
  private def handleLobby(ctx: RoutingContext): Unit =
//     missing JSON OF Player username
//    val name = ctx.body().asString()
    val lobbyId = ctx.request().extractLobbyId.getOrElse(LobbyId.generate)
    val player = Player.human("name")
    lobbyStatePort.addPlayer(lobbyId, player).onComplete:
      case Success(playerIdOpt) => playerIdOpt match
        case Some(playerId) =>
          val lobbyPlayer = LobbyPlayer(playerId, player)
          lobbyStatePort.getLobby(lobbyId).onComplete:
            case Success(lobbyOpt) => 
              val lobby = lobbyOpt match
                case Some(value) => value.addPlayer(lobbyPlayer)
                case None => Lobby(lobbyId, List(lobbyPlayer), LobbyStatus.WAITING)
              lobbyStatePort.saveLobby(lobby)
              respondJson(ctx, 201, Json.obj("lobbyId" -> lobbyId.asJson, "playerId" -> playerId.asJson)) // todo: perchè 201?
            case Failure(exception) => ???
        case None => ??? //ctx.fail(401 (unauthorized), "lobby is full")
      case Failure(exception) => ctx.fail(500, exception)

  private def handleMissingLobby(ctx: RoutingContext): Unit =
    respondJson(ctx, 400, Json.obj("error" -> "Missing lobby id".asJson))

  private def handleLobbyInfo(ctx: RoutingContext): Unit =
    ctx.request().extractLobbyId match
      case Some(lobbyId) =>     
        lobbyStatePort.getLobby(lobbyId).onComplete:
          case Success(Some(lobby)) =>
            println(lobby)
            ??? // respondJson(ctx, 200, lobby.asJson) todo: missing codec
          case Success(None)        => ??? //respondJson(ctx, 404, notFound(lobbyId.asJson)) todo: import codec
          case Failure(exception)   => ctx.fail(500, exception) 
      case None => ???


  private def handleStartGame(ctx: RoutingContext): Unit =
    ctx.request().extractLobbyId match
      case Some(lobbyId) =>
        lobbyStatePort.getLobby(lobbyId).onComplete:
          case Success(Some(lobby)) =>
            if lobby.status == LobbyStatus.WAITING then
              lobbyStatePort
                .saveLobby(lobby.copy(status = LobbyStatus.IN_GAME))
                .onComplete:
                  case Success(_) =>
                    gameEngine
                      .startGame(lobbyId, lobby.players.map(_.id), GameConfiguration(1000, lobby.players))
                      .onComplete:
                        case Success(_) =>
                          respondJson(ctx, 200, Json.obj("message" -> "Game started".asJson))
                          println(s"Game started for lobby $lobby")
                        case Failure(exception) => println(s"Failed to start game for lobby $lobbyId: ${exception.getMessage}")
                  case Failure(exception) => ctx.fail(500, exception)
            else
              respondJson(ctx, 400, Json.obj("error" -> "Game already started or finished".asJson))
          case Success(None)      => ??? // respondJson(ctx, 404, notFound(lobbyId.asJson)) todo: missing import codec
          case Failure(exception) => ctx.fail(500, exception)
      case None => ???

//  private def notFound(uuid: String): Json =
//    Json.obj("error" -> s"Lobby $uuid not found".asJson)

  def respondJson(ctx: RoutingContext, status: Int, message: Json): Unit =
//    val body = status match
//      case 200 | 201 => ???
//      case _         => ???

    ctx
      .response()
      .setStatusCode(status)
      .putHeader("Content-Type", "application/json")
      .end(message.noSpaces)