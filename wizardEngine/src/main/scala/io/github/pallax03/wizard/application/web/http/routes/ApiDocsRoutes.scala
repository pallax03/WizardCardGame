package io.github.pallax03.wizard.application.web.http.routes

import scala.concurrent.Future

import io.github.pallax03.wizard.application.web.ApiSupport
import io.github.pallax03.wizard.application.web.ws.WebSocketEndpoints

import sttp.apispec.asyncapi.circe.yaml.*
import sttp.apispec.openapi.Info
import sttp.tapir.*
import sttp.tapir.docs.asyncapi.AsyncAPIInterpreter
import sttp.tapir.files.*
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.swagger.bundle.SwaggerInterpreter

/** Documentation portal routes serving OpenAPI (Swagger UI) and AsyncAPI (WebSocket UI + YAML). */
class ApiDocsRoutes(domainEndpoints: List[ServerEndpoint[Any, Future]]):

  private val openApiInfo = Info(
    title = "Wizard Game Engine REST API",
    version = ApiSupport.ApiVersion,
    description = Some(
      s"REST ${ApiSupport.VersionSegment} specification. Real-time WebSocket AsyncAPI specification is available at " +
        s"[${ApiSupport.AsyncApiUiPath}](${ApiSupport.AsyncApiUiPath}) ([YAML](${ApiSupport.AsyncApiYamlPath}))."
    )
  )

  private val asyncApiYaml: String =
    AsyncAPIInterpreter()
      .toAsyncAPI(
        WebSocketEndpoints.lobbyEvents,
        "Wizard Game Engine WebSocket API",
        ApiSupport.ApiVersion
      )
      .toYaml

  private val asyncApiYamlEndpoint: ServerEndpoint[Any, Future] =
    endpoint.get
      .in(ApiSupport.DocsPrefix / ApiSupport.AsyncApiYamlFile)
      .out(stringBody)
      .serverLogicSuccess(_ => Future.successful(asyncApiYaml))

  private val asyncApiUiEndpoint: ServerEndpoint[Any, Future] =
    staticResourceGetServerEndpoint[Future](ApiSupport.DocsPrefix / ApiSupport.AsyncApiUiSegment)(
      classOf[ApiDocsRoutes].getClassLoader,
      "asyncapi.html"
    )

  private val swaggerEndpoints: List[ServerEndpoint[Any, Future]] =
    SwaggerInterpreter().fromServerEndpoints(domainEndpoints, openApiInfo)

  val all: List[ServerEndpoint[Any, Future]] =
    List(asyncApiYamlEndpoint, asyncApiUiEndpoint) ++ swaggerEndpoints
