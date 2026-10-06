package io.github.pallax03.wizard.application.web.http.routes

import scala.concurrent.{ExecutionContext, Future}

import cats.data.EitherT
import cats.implicits.*

import io.github.pallax03.wizard.application.web.http.endpoints.AIEndpoints
import io.github.pallax03.wizard.engine.lobby.{LobbyError, LobbyId}
import io.github.pallax03.wizard.engine.model.basic.PlayerId
import io.github.pallax03.wizard.engine.ports.{AIError, AIPort, LobbyStatePort}

import sttp.tapir.server.ServerEndpoint

/**
 * Tapir HTTP server routes connecting AI advisor endpoints ([[AIEndpoints]]) to the underlying [[AIPort]].
 *
 * Implements authentication validation against [[LobbyStatePort]] prior to querying the Prolog rule engine
 * for optimal game moves. Translates internal [[AIError]] states into client-facing [[LobbyError]] responses:
 *  - Missing games or missing player states are mapped to [[LobbyError.NotFound]].
 *  - Uncomputable moves or solver failures are mapped to [[LobbyError.IAHintError]].
 *  - Empty hint results are rejected with [[AIError.NoHintFound]].
 *
 * Exposes all endpoints via [[all]] for registration into [[HttpServerVerticle]].
 */
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
        case AIError.GameException(err) => LobbyError.NotFound(err)
        case err                        => LobbyError.IAHintError(err)
      }
      aiResult <- EitherT.fromOption[Future](
        aiResultOpt,
        LobbyError.IAHintError(AIError.NoHintFound)
      )
    yield aiResult).value

  private val hintBestTrump: ServerEndpoint[Any, Future] =
    AIEndpoints.bestTrump
      .serverSecurityLogicSuccess(Future.successful)
      .serverLogic(secret =>
        lobbyId => handleHint(secret, lobbyId, aiPort.resolvedTrumpColor(lobbyId, _))
      )

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
