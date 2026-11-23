package io.github.pallax03.wizard.application.web.ws

import io.github.pallax03.wizard.application.web.ApiSupport
import io.github.pallax03.wizard.codecs.ws.WsMessageCodecs.given
import io.github.pallax03.wizard.engine.lobby.{LobbyError, LobbyId}

import sttp.capabilities.WebSockets
import sttp.tapir.*
import sttp.tapir.json.circe.*
import sttp.tapir.server.vertx.streams.VertxStreams

/**
 * Declarative Tapir WebSocket endpoint contract for real-time lobby event streaming.
 *
 * Serves as the single source of truth for path versioning, authentication query parameters,
 * error responses, and automated AsyncAPI specification generation.
 */
object WebSocketEndpoints:

  val SecretParam: String = "secret"

  val lobbyEvents: Endpoint[
    Unit,
    (LobbyId, String),
    LobbyError,
    VertxStreams.Pipe[ChatMessage, WsServerMessage],
    VertxStreams & WebSockets
  ] =
    ApiSupport.baseWsLobbyEndpoint
      .in(query[String](SecretParam).description("Player authentication secret token"))
      .tag("WebSocket")
      .summary("Lobby real-time event stream & chat")
      .description(
        "Bidirectional WebSocket channel streaming WizardEvents, SystemEvents, and player ChatMessages."
      )
      .out(
        webSocketBody[ChatMessage, CodecFormat.Json, WsServerMessage, CodecFormat.Json](
          VertxStreams
        )
      )

  /** Derives the Vert.x router path template from the Tapir endpoint definition. */
  val vertxRoutePath: String =
    lobbyEvents.showPathTemplate(
      showPathParam = (_, p) => s":${p.name.getOrElse("param")}",
      showQueryParam = None,
      includeAuth = false,
      showNoPathAs = "/"
    )
