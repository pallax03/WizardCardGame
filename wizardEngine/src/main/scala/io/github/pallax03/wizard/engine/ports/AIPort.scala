package io.github.pallax03.wizard.engine.ports

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

import io.github.pallax03.wizard.engine.lobby.LobbyId
import io.github.pallax03.wizard.engine.model.basic.PlayerId
import io.github.pallax03.wizard.engine.model.basic.bidding.Bid
import io.github.pallax03.wizard.engine.model.basic.cards.Card
import io.github.pallax03.wizard.engine.model.core.state.PlayerGameState
import io.github.pallax03.wizard.engine.model.core.{
  AbortedGameException,
  EntityNotFound,
  GameException,
  RecoveredGameException
}

/** Failure modes encountered during AI decision inference. */
enum AIError:
  /** The requested action cannot be evaluated because the game is in an unexpected phase. */
  case InvalidPhase(actualGameState: PlayerGameState)

  /** Target lobby or player was not found in the game engine. */
  case GameException(entityNotFound: EntityNotFound)

  /** The AI reasoning engine could not produce a valid move recommendation. */
  case NoHintFound

/**
 * Service port for AI decision-making and bot strategy evaluation.
 *
 * Employs a template pattern: queries [[PlayerGameState]] through [[InboundPort]], verifies
 * that the game is in the appropriate phase, and delegates strategic inference to concrete
 * engine implementations (such as the Prolog rule engine).
 */
trait AIPort(inboundPort: InboundPort):

  private def onRunningPhase[T](lobbyId: LobbyId)(playerId: PlayerId)(
      phaseLogic: PartialFunction[PlayerGameState, T]
  ): Future[Either[AIError, T]] =
    inboundPort
      .getState(lobbyId, playerId)
      .map: state =>
        phaseLogic
          .andThen(Right(_))
          .applyOrElse(
            state,
            _ => Left(AIError.InvalidPhase(state))
          )
      .recover {
        case e: EntityNotFound                         => Left(AIError.GameException(e))
        case AbortedGameException(e: EntityNotFound)   => Left(AIError.GameException(e))
        case RecoveredGameException(e: EntityNotFound) => Left(AIError.GameException(e))
        case _: GameException | _: AbortedGameException | _: RecoveredGameException =>
          Left(AIError.NoHintFound)
      }

  protected def resolveTrumpColorLogic(
      playerId: PlayerId
  ): PartialFunction[PlayerGameState, Option[Card.Color]]
  protected def placeBidLogic(playerId: PlayerId): PartialFunction[PlayerGameState, Option[Bid]]
  protected def bestCardLogic(playerId: PlayerId): PartialFunction[PlayerGameState, Option[Card]]

  /**
   * Evaluates the optimal trump color when a Wizard card is revealed as the trump at round start.
   *
   * @param playerId dealer responsible for selecting the trump color.
   */
  final def resolvedTrumpColor(
      lobbyId: LobbyId,
      playerId: PlayerId
  ): Future[Either[AIError, Option[Card.Color]]] =
    onRunningPhase(lobbyId)(playerId)(resolveTrumpColorLogic(playerId))

  /** Evaluates the optimal bid for the current round given the player's dealt hand and trump suit. */
  final def placeBid(lobbyId: LobbyId, playerId: PlayerId): Future[Either[AIError, Option[Bid]]] =
    onRunningPhase(lobbyId)(playerId)(placeBidLogic(playerId))

  /** Selects the optimal legal card to play given current table plays, trick history, and trump suit. */
  final def bestCard(lobbyId: LobbyId, playerId: PlayerId): Future[Either[AIError, Option[Card]]] =
    onRunningPhase(lobbyId)(playerId)(bestCardLogic(playerId))
