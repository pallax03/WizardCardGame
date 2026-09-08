package io.github.pallax03.wizard.application.web.http.routes

import scala.concurrent.{ExecutionContext, Future}

import io.github.pallax03.wizard.application.web.http.endpoints.AIEndpoints
import io.github.pallax03.wizard.engine.lobby.{LobbyError, LobbyId}
import io.github.pallax03.wizard.engine.model.basic.PlayerId
import io.github.pallax03.wizard.engine.ports.{AIPort, LobbyStatePort}

import sttp.tapir.server.ServerEndpoint

class AIRoutes(lobbyStatePort: LobbyStatePort, aiPort: AIPort)(using ec: ExecutionContext):

  private def handleHint[A](
      secret: String,
      lobbyId: LobbyId,
      action: PlayerId => Future[A]
  ): Future[Either[LobbyError, A]] =
    lobbyStatePort
      .getLobby(lobbyId)
      .flatMap:
        case Some(lobby) =>
          lobby.authenticate(secret) match
            case Right(player) =>
              action(player.id).map(Right(_))
            case Left(err) =>
              Future.successful(Left(err))
        case None =>
          Future.successful(Left(LobbyError.LobbyNotFound))
      .recover:
        case ex: Throwable =>
          Left(LobbyError.GameActionRejected(ex.getMessage))

  private val hintBestTrump: ServerEndpoint[Any, Future] =
    AIEndpoints.bestTrump
      .serverSecurityLogicSuccess(secret => Future.successful(secret))
      .serverLogic(secret =>
        lobbyId =>
          handleHint(
            secret,
            lobbyId,
            playerId => aiPort.resolvedTrumpColor(lobbyId, playerId)
          )
      )

  private val hintBestBid: ServerEndpoint[Any, Future] =
    AIEndpoints.bestBid
      .serverSecurityLogicSuccess(secret => Future.successful(secret))
      .serverLogic(secret =>
        lobbyId => handleHint(secret, lobbyId, playerId => aiPort.placeBid(lobbyId, playerId))
      )

  private val hintBestCard: ServerEndpoint[Any, Future] =
    AIEndpoints.bestCard
      .serverSecurityLogicSuccess(secret => Future.successful(secret))
      .serverLogic(secret =>
        lobbyId => handleHint(secret, lobbyId, playerId => aiPort.bestCard(lobbyId, playerId))
      )

  val all: List[ServerEndpoint[Any, Future]] = List(
    hintBestTrump,
    hintBestBid,
    hintBestCard
  )
