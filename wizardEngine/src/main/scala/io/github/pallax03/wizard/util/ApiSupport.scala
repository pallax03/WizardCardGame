package io.github.pallax03.wizard.util

import io.github.pallax03.wizard.codecs.engine.lobby.LobbyCodecs.given
import io.github.pallax03.wizard.engine.lobby.{LobbyError, LobbyId}

import sttp.model.StatusCode
import sttp.tapir.*
import sttp.tapir.json.circe.*

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
  val LobbiesResource: String = "lobbies"

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
    case LobbyError.NotFound(_) | LobbyError.LobbyNotFound => StatusCode.NotFound
    case LobbyError.NotAuthenticated                       => StatusCode.Unauthorized
    case _                                                 => StatusCode.BadRequest

  /** Shared error output: maps [[LobbyError]] to 400 / 401 / 404 for OpenAPI/AsyncAPI. */
  val errorOutput: EndpointOutput[LobbyError] =
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
