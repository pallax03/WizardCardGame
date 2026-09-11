package io.github.pallax03.wizard.application.web.http.routes

import scala.concurrent.{ExecutionContext, Future}

import cats.data.EitherT
import cats.implicits.*

import io.github.pallax03.wizard.application.web.http.endpoints.AIEndpoints
import io.github.pallax03.wizard.engine.lobby.{LobbyError, LobbyId}
import io.github.pallax03.wizard.engine.model.basic.PlayerId
import io.github.pallax03.wizard.engine.ports.{AIError, AIPort, LobbyStatePort}
import sttp.tapir.server.ServerEndpoint

class AIRoutes(lobbyStatePort: LobbyStatePort, aiPort: AIPort)(using ec: ExecutionContext):

  private def handleHint[A](
      secret: String,
      lobbyId: LobbyId,
      action: PlayerId => Future[Either[AIError, Option[A]]]
  ): Future[Either[LobbyError, A]] =
    (for
      playerPair <- EitherT(lobbyStatePort.getAuthLobby(lobbyId, secret))
      player = playerPair._1
      aiResultOpt <- EitherT(action(player.id)).leftMap {
        case AIError.InvalidPhase(msg) => LobbyError.GameActionRejected(msg)
        case AIError.PlayerNotFound(_) => LobbyError.PlayerNotFound
      }
      aiResult <- EitherT.fromOption[Future](aiResultOpt, LobbyError.GameActionRejected("No AI hint available"))
    yield aiResult).value

  private val hintBestTrump: ServerEndpoint[Any, Future] =
    AIEndpoints.bestTrump
      .serverSecurityLogicSuccess(Future.successful)
      .serverLogic(secret => lobbyId => handleHint(secret, lobbyId, aiPort.resolvedTrumpColor(lobbyId, _)))

  private val hintBestBid: ServerEndpoint[Any, Future] =
    AIEndpoints.bestBid
      .serverSecurityLogicSuccess(Future.successful)
      .serverLogic(secret => lobbyId => handleHint(secret, lobbyId, aiPort.placeBid(lobbyId, _)))

  private val hintBestCard: ServerEndpoint[Any, Future] =
    AIEndpoints.bestCard
      .serverSecurityLogicSuccess(Future.successful)
      .serverLogic(secret => lobbyId => handleHint(secret, lobbyId, aiPort.bestCard(lobbyId, _)))

  val all: List[ServerEndpoint[Any, Future]] = List(
    hintBestTrump,
    hintBestBid,
    hintBestCard
  )
