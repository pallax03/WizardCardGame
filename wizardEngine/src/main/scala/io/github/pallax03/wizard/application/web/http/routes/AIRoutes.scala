package io.github.pallax03.wizard.application.web.http.routes

import scala.concurrent.{ExecutionContext, Future}

import io.github.pallax03.wizard.application.web.http.ActionSuccessResponse
import io.github.pallax03.wizard.application.web.http.endpoints.AIEndpoints
import io.github.pallax03.wizard.codecs.engine.model.basic.CardCodecs.given
import io.github.pallax03.wizard.codecs.syntax.CodecSyntax.*
import io.github.pallax03.wizard.engine.errors.AppError
import io.github.pallax03.wizard.engine.lobby.LobbyId
import io.github.pallax03.wizard.engine.model.basic.PlayerId
import io.github.pallax03.wizard.engine.ports.{AIPort, LobbyStatePort}

import sttp.tapir.server.ServerEndpoint

class AIRoutes(lobbyStatePort: LobbyStatePort, aiPort: AIPort)(using ec: ExecutionContext):

  private def handleHint[A](
      secret: String,
      lobbyId: LobbyId,
      action: PlayerId => Future[A],
      encode: A => String
  ): Future[Either[AppError, ActionSuccessResponse]] =
    lobbyStatePort
      .getLobby(lobbyId)
      .flatMap:
        case Some(lobby) =>
          lobby.players.find(_.secret.contains(secret)) match
            case Some(player) =>
              action(player.id).map(res => Right(ActionSuccessResponse(encode(res))))
            case None =>
              Future.successful(Left(AppError.NotAuthenticated))
        case None =>
          Future.successful(Left(AppError.LobbyNotFound(lobbyId)))
      .recover:
        case ex: Throwable =>
          Left(AppError.InternalServerError(ex.getMessage))

  private val hintBestTrump: ServerEndpoint[Any, Future] =
    AIEndpoints.bestTrump
      .serverSecurityLogicSuccess(secret => Future.successful(secret))
      .serverLogic(secret =>
        lobbyId =>
          handleHint(
            secret,
            lobbyId,
            playerId => aiPort.resolvedTrumpColor(lobbyId, playerId),
            _.toJson
          )
      )

  private val hintBestBid: ServerEndpoint[Any, Future] =
    AIEndpoints.bestBid
      .serverSecurityLogicSuccess(secret => Future.successful(secret))
      .serverLogic(secret =>
        lobbyId =>
          handleHint(secret, lobbyId, playerId => aiPort.placeBid(lobbyId, playerId), _.toJson)
      )

  private val hintBestCard: ServerEndpoint[Any, Future] =
    AIEndpoints.bestCard
      .serverSecurityLogicSuccess(secret => Future.successful(secret))
      .serverLogic(secret =>
        lobbyId =>
          handleHint(secret, lobbyId, playerId => aiPort.bestCard(lobbyId, playerId), _.toJson)
      )

  val all: List[ServerEndpoint[Any, Future]] = List(
    hintBestTrump,
    hintBestBid,
    hintBestCard
  )
