package io.github.pallax03.wizard.engine.ports

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

import io.github.pallax03.wizard.engine.lobby.LobbyId
import io.github.pallax03.wizard.engine.model.basic.PlayerId
import io.github.pallax03.wizard.engine.model.basic.bidding.Bid
import io.github.pallax03.wizard.engine.model.basic.cards.Card
import io.github.pallax03.wizard.engine.model.core.state.PlayerGameState

enum AIError:
  case InvalidPhase(actualGameState: PlayerGameState)
  case PlayerNotFound
  case NoHintFound

/**
 * Defines the interface for an AI component capable of making decisions within the Wizard game.
 *
 * Implementations of this port are responsible for querying game logic
 * to provide valid actions based on the current [[GameState]].
 *
 * Each method is asynchronous, returning a [[Future]] to ensure the game engine
 * remains responsive while the AI computes its strategy.
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
      .recover { case _ => Left(AIError.PlayerNotFound) }

  protected def resolveTrumpColorLogic(
      playerId: PlayerId
  ): PartialFunction[PlayerGameState, Option[Card.Color]]
  protected def placeBidLogic(playerId: PlayerId): PartialFunction[PlayerGameState, Option[Bid]]
  protected def bestCardLogic(playerId: PlayerId): PartialFunction[PlayerGameState, Option[Card]]

  /**
   * Selects the best trump color to resolve a Wizard card.
   *
   * @param lobbyId the ID of the lobby.
   * @param playerId the ID of the dealer who needs to resolve the trump.
   * @return A [[Future]] containing the chosen [[Card.Color]].
   */
  final def resolvedTrumpColor(
      lobbyId: LobbyId,
      playerId: PlayerId
  ): Future[Either[AIError, Option[Card.Color]]] =
    onRunningPhase(lobbyId)(playerId)(resolveTrumpColorLogic(playerId))

  /**
   * Determines the bid for the current round.
   *
   * @param lobbyId the ID of the lobby.
   * @param playerId the ID of the player placing the bid.
   * @return A [[Future]] containing the suggested [[Bid]].
   */
  final def placeBid(lobbyId: LobbyId, playerId: PlayerId): Future[Either[AIError, Option[Bid]]] =
    onRunningPhase(lobbyId)(playerId)(placeBidLogic(playerId))

  /**
   * Selects the optimal card to play from the player's hand given the current table state.
   *
   * @param lobbyId the ID of the lobby.
   * @param playerId the ID of the player whose turn it is.
   * @return A [[Future]] containing the selected [[Card]].
   */
  final def bestCard(lobbyId: LobbyId, playerId: PlayerId): Future[Either[AIError, Option[Card]]] =
    onRunningPhase(lobbyId)(playerId)(bestCardLogic(playerId))
