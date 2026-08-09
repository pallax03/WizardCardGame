package it.unibo.pps.wizard.application.web

import io.vertx.core.http.HttpServerRequest
import it.unibo.pps.wizard.engine.lobby.LobbyId
import it.unibo.pps.wizard.engine.model.basic.PlayerId

/**
 * Object utility to extract parameters from an HTTP request.
 *
 * Expected url: <host>/lobby/{lobbyId}/player/{playerId}
 */
extension (request: HttpServerRequest)
  /**
   * Extracts a `LobbyID` from an HTTP request.
   *
   * It expects the `lobbyId` to be present as a path parameter in the request.
   *
   * @return An `Option` containing the extracted `LobbyID` if present, otherwise `None`.
   */
  def extractLobbyId: Option[LobbyId] =
    Option(request.getParam("lobbyId")).map(LobbyId(_))

  /**
   * Extracts a `PlayerID` from an HTTP request.
   *
   * It expects the `playerId` to be present as a query parameter in the request.
   *
   * @return An `Option` containing the extracted `PlayerID` if present, otherwise `None`.
   */
  def extractPlayerId: Option[PlayerId] =
    Option(request.getParam("playerId")).flatMap(_.toIntOption).map(PlayerId(_))
