package io.github.pallax03.wizard.application.web

import scala.concurrent.Future

import io.github.pallax03.wizard.codecs.engine.lobby.LobbyCodecs.given
import io.github.pallax03.wizard.engine.lobby.{LobbyError, LobbyId}
import io.github.pallax03.wizard.engine.model.core.{
  AbortedGameException,
  GameException,
  RecoveredGameException
}
import io.github.pallax03.wizard.util.WizardLogger

import sttp.model.StatusCode
import sttp.tapir.*
import sttp.tapir.json.circe.*
import sttp.tapir.server.interceptor.exception.ExceptionHandler
import sttp.tapir.server.model.ValuedEndpointOutput

/**
 * Shared API protocol definitions, version metadata, path builders, and error mappings
 * across HTTP REST and WebSocket endpoints.
 */
object ApiSupport:

  private val ApiMajorVersion: Int = 1
  val VersionSegment: String = s"v$ApiMajorVersion"
  val ApiVersion: String =
    Option(getClass.getPackage.getImplementationVersion).getOrElse(s"$ApiMajorVersion.0.0")

  val LobbyIdParam: String = "lobbyId"
  private val LobbiesResource: String = "lobbies"

  val LobbiesBasePath: String = s"/api/$VersionSegment/$LobbiesResource"
  def lobbyPath(lobbyId: LobbyId | String): String = s"$LobbiesBasePath/$lobbyId"
  def lobbyHintPath(lobbyId: LobbyId | String): String = s"${lobbyPath(lobbyId)}/hint"

  val WsLobbyChannelTemplate: String = s"/ws/$VersionSegment/$LobbiesResource/{$LobbyIdParam}"
  val WsLobbyRoutePattern: String = s"/ws/$VersionSegment/$LobbiesResource/:$LobbyIdParam"

  val DocsPrefix: String = "docs"
  val AsyncApiYamlFile: String = "asyncapi.yaml"
  val AsyncApiUiSegment: String = "asyncapi"
  val AsyncApiYamlPath: String = s"/$DocsPrefix/$AsyncApiYamlFile"
  val AsyncApiUiPath: String = s"/$DocsPrefix/$AsyncApiUiSegment"

  /** Typed path extractor for `/{lobbyId}`. */
  val lobbyIdPath: EndpointInput[LobbyId] =
    path[String](LobbyIdParam).map(LobbyId.apply)(_.toString)

  /** Maps a [[LobbyError]] to its corresponding HTTP [[StatusCode]]. */
  def statusCodeFor(error: LobbyError): StatusCode = error match
    case LobbyError.LobbyNotFound | LobbyError.GameNotFound | LobbyError.PlayerNotFound(_) =>
      StatusCode.NotFound
    case LobbyError.NotAuthenticated       => StatusCode.Unauthorized
    case LobbyError.InternalServerError(_) => StatusCode.InternalServerError
    case _                                 => StatusCode.BadRequest

  /** Extracts `lobbyId` from the request path segments following the `lobbies` resource segment. */
  private def extractLobbyId(segments: Seq[String]): Option[String] =
    segments.dropWhile(_ != LobbiesResource).drop(1).headOption.filter(_.nonEmpty)

  /** Maps an unhandled domain or recovery exception to a log message and client [[LobbyError]]. */
  private def mapException(endpointShow: String, ex: Throwable): (String, LobbyError) = ex match
    case rge: RecoveredGameException =>
      (
        s"RECOVERED GameException (${rge.ge}), endpoint: $endpointShow",
        LobbyError.InternalServerError("GAME_RECOVERED")
      )
    case age: AbortedGameException =>
      (
        s"ABORTED GameException (${age.ge}), endpoint: $endpointShow",
        LobbyError.InternalServerError("GAME_ABORTED")
      )
    case ge: GameException =>
      (
        s"NOT HANDLED GameException (${ge.getMessage}), endpoint: $endpointShow",
        LobbyError.InternalServerError("GAME_EXCEPTION")
      )
    case _ =>
      (
        s"CRITICAL $endpointShow",
        LobbyError.InternalServerError("INTERNAL_ERROR")
      )

  /** Centralized Tapir server exception handler mapping domain and recovery failures to JSON responses. */
  val exceptionHandler: ExceptionHandler[Future] = ExceptionHandler[Future](ctx =>
    val lobbyIdOpt = extractLobbyId(ctx.request.pathSegments)
    WizardLogger.withContext(lobbyIdOpt):
      val (logMsg, lobbyError) = mapException(ctx.endpoint.show, ctx.e)
      WizardLogger.error(logMsg, ctx.e)
      val status = statusCodeFor(lobbyError)
      Future.successful(
        Some(ValuedEndpointOutput(jsonBody[LobbyError].and(statusCode(status)), lobbyError))
      )
  )

  /** Shared error output: maps [[LobbyError]] to 400 / 401 / 404 for OpenAPI/AsyncAPI. */
  private val errorOutput: EndpointOutput[LobbyError] =
    oneOf[LobbyError](
      oneOfVariantValueMatcher(StatusCode.NotFound, jsonBody[LobbyError]) {
        case err: LobbyError if statusCodeFor(err) == StatusCode.NotFound => true
      },
      oneOfVariantValueMatcher(StatusCode.Unauthorized, jsonBody[LobbyError]) {
        case err: LobbyError if statusCodeFor(err) == StatusCode.Unauthorized => true
      },
      oneOfVariantValueMatcher(StatusCode.BadRequest, jsonBody[LobbyError]) { case _: LobbyError =>
        true
      }
    )

  /** Shared public base endpoint for lobby REST resources. */
  val baseLobbyEndpoint: Endpoint[Unit, Unit, LobbyError, Unit, Any] =
    endpoint.in("api" / VersionSegment / LobbiesResource).errorOut(errorOutput)

  /** Shared authenticated base endpoint for a specific lobby REST resource. */
  val secureLobbyEndpoint: Endpoint[String, LobbyId, LobbyError, Unit, Any] =
    baseLobbyEndpoint.securityIn(auth.bearer[String]()).in(lobbyIdPath)

  /** Shared base WebSocket endpoint for a specific lobby event stream. */
  val baseWsLobbyEndpoint: Endpoint[Unit, LobbyId, LobbyError, Unit, Any] =
    endpoint.get.in("ws" / VersionSegment / LobbiesResource / lobbyIdPath).errorOut(errorOutput)
