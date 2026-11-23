package io.github.pallax03.wizard.application.web.http.routes

import java.io.StringReader
import javax.xml.parsers.DocumentBuilderFactory

import scala.concurrent.ExecutionContext.Implicits.global

import io.github.pallax03.wizard.application.web.ApiSupport
import io.github.pallax03.wizard.application.web.http.HttpTestDSL
import io.github.pallax03.wizard.application.web.ws.WebSocketEndpoints
import io.github.pallax03.wizard.engine.lobby.LobbyError
import io.github.pallax03.wizard.engine.model.basic.PlayerId
import io.github.pallax03.wizard.engine.ports.{InMemoryLobbyStatePort, StubAIPort, StubInboundPort}

import org.xml.sax.InputSource
import sttp.apispec.asyncapi.circe.yaml.*
import sttp.apispec.openapi.circe.yaml.*
import sttp.model.StatusCode
import sttp.tapir.docs.asyncapi.AsyncAPIInterpreter
import sttp.tapir.docs.openapi.OpenAPIDocsInterpreter

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class TestApiDocsRoutes extends AnyWordSpec with Matchers with HttpTestDSL:

  private val lobbyPort = InMemoryLobbyStatePort()
  private val inboundPort = StubInboundPort()
  private val aiPort = StubAIPort(inboundPort)

  private val domainEndpoints =
    LobbyRoutes(lobbyPort, inboundPort).all ++
      ActionRoutes(lobbyPort, inboundPort).all ++
      AIRoutes(lobbyPort, aiPort).all

  "API Specifications" should:
    "version all REST endpoints under ApiSupport.LobbiesBasePath in OpenAPI" in:
      val openApi = OpenAPIDocsInterpreter()
        .toOpenAPI(domainEndpoints.map(_.endpoint), "Wizard", ApiSupport.ApiVersion)
      val paths = openApi.paths.pathItems.keys.toList
      paths should not be empty
      all(paths) should startWith(ApiSupport.LobbiesBasePath)

      val yaml = openApi.toYaml
      yaml should include("PlayerGameState")
      yaml should include("Scoreboard")
      yaml should include("Trump")
      yaml should include("ErrorResponse")
      yaml should include("BoundsErrorResponse")

    "expose versioned WebSocket channel in AsyncAPI and Vert.x router" in:
      WebSocketEndpoints.vertxRoutePath shouldBe ApiSupport.WsLobbyRoutePattern

      val asyncApi = AsyncAPIInterpreter().toAsyncAPI(
        WebSocketEndpoints.lobbyEvents,
        "Wizard Game Engine WebSocket API",
        ApiSupport.ApiVersion
      )
      asyncApi.channels.keys should contain(ApiSupport.WsLobbyChannelTemplate)
      val yaml = asyncApi.toYaml
      yaml should include("WizardEvent")
      yaml should include("SystemEvent")
      yaml should include("ChatMessage")
      yaml should include("GameStarted")

    "map LobbyError variants consistently via ApiSupport.statusCodeFor" in:
      ApiSupport.statusCodeFor(LobbyError.LobbyNotFound) shouldBe StatusCode.NotFound
      ApiSupport.statusCodeFor(LobbyError.GameNotFound) shouldBe StatusCode.NotFound
      ApiSupport.statusCodeFor(LobbyError.PlayerNotFound(PlayerId(1))) shouldBe StatusCode.NotFound
      ApiSupport.statusCodeFor(LobbyError.NotAuthenticated) shouldBe StatusCode.Unauthorized
      ApiSupport.statusCodeFor(LobbyError.GamePaused) shouldBe StatusCode.BadRequest
      ApiSupport.statusCodeFor(LobbyError.InternalServerError("INTERNAL_ERROR")) shouldBe
        StatusCode.InternalServerError

    "serve AsyncAPI YAML and valid XHTML UI via ApiDocsRoutes" in:
      withEndpoints(ApiDocsRoutes(domainEndpoints).all):
        val yamlRes = get(ApiSupport.AsyncApiYamlPath)
        yamlRes.code shouldBe StatusCode.Ok
        yamlRes.body.value should include(ApiSupport.WsLobbyChannelTemplate)

        val uiRes = get(ApiSupport.AsyncApiUiPath)
        uiRes.code shouldBe StatusCode.Ok
        val html = uiRes.body.value
        html should include(ApiSupport.AsyncApiYamlPath)
        html should include("AsyncApiStandalone.render")

        val doc = DocumentBuilderFactory
          .newInstance()
          .newDocumentBuilder()
          .parse(InputSource(StringReader(html)))
        doc.getDocumentElement.getTagName shouldBe "html"
        doc.getDocumentElement.getAttribute("data-theme") shouldBe "dark"
