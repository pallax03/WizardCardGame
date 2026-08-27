package io.github.pallax03.wizard.application.web.http.routes

import io.github.pallax03.wizard.engine.ports.LobbyStatePort
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext

class WizardAIRoutes(lobbyStatePort: LobbyStatePort):

  def mount(router: Router): Unit =
    router.get("/api/lobby/:lobbyId/player/:playerId/hit/choose").handler(???)
    router.get("/api/lobby/:lobbyId/player/:playerId/hit/bid").handler(???)
    router.get("/api/lobby/:lobbyId/player/:playerId/hit/bid?bid=1").handler(???)
    router.get("/api/lobby/:lobbyId/player/:playerId/hit/card").handler(???)

  def handleAIAction(ctx: RoutingContext): Unit =
    println(lobbyStatePort)
    val lobbyId = ctx.pathParam("lobbyId")
    val playerId = ctx.pathParam("playerId")
    // Here you would implement the logic to determine the AI's action based on the game state
    // For now, we will just return a placeholder response
    ctx
      .response()
      .setStatusCode(200)
      .putHeader("Content-Type", "application/json")
      .end(s"""{"message": "AI action for lobby $lobbyId and player $playerId"}""")
