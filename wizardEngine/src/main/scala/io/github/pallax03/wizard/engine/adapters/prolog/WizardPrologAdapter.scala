package io.github.pallax03.wizard.engine.adapters.prolog

import io.github.pallax03.wizard.engine.adapters.prolog.WizardPrologEngine
import io.github.pallax03.wizard.engine.lobby.LobbyId
import io.github.pallax03.wizard.engine.model.basic.PlayerId
import io.github.pallax03.wizard.engine.model.basic.bidding.Bid
import io.github.pallax03.wizard.engine.model.basic.bidding.Bids
import io.github.pallax03.wizard.engine.model.basic.cards.Card
import io.github.pallax03.wizard.engine.model.basic.cards.Hand
import io.github.pallax03.wizard.engine.model.basic.gameplay.Round
import io.github.pallax03.wizard.engine.model.basic.gameplay.Table
import io.github.pallax03.wizard.engine.model.core.state.GameState
import io.github.pallax03.wizard.engine.model.core.state.PlayerGameState
import io.github.pallax03.wizard.engine.model.rules.BiddingRules._
import io.github.pallax03.wizard.engine.model.rules.TableRules._
import io.github.pallax03.wizard.engine.ports.AIPort
import io.github.pallax03.wizard.engine.ports.InboundPort

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
 * Adapter that connects the game engine's AI requirements with the Prolog knowledge base.
 *
 * This Adapter work with [[GameEngineInboundAdapter]] as every api need to get actual state to respond with the correct data for the correct playerId request.
 *
 * @throws Future[Exception] Each api return a failed future if any problem occurs.
 *
 * This component acts as a safety layer:
 * 1. Validates that the AI requests are performed during the correct game phases.
 * 2. Manages interactions with the [[WizardPrologEngine]].
 * 3. Provides robust fallbacks: if Prolog fails to return a valid move, this adapter ensures the game continues by providing a valid default move.
 */
class WizardPrologAdapter(private val inboundPort: InboundPort) extends AIPort:

  private val engine = WizardPrologEngine()

  private def onRunningPhase[T](lobbyId: LobbyId, actionName: String)(playerId: PlayerId)(
      phaseLogic: PartialFunction[PlayerGameState, Future[T]]
  ): Future[T] =
    inboundPort
      .getState(lobbyId, playerId)
      .flatMap: state =>
        phaseLogic.applyOrElse(
          state,
          _ => Future.failed(IllegalStateException(s"Cannot $actionName: invalid game phase"))
        )

  private def withHand[T](handOpt: Option[Hand])(prologLogic: Hand => T): Future[T] =
    handOpt match
      case Some(hand) => Future.successful(prologLogic(hand))
      case None       => Future.failed(IllegalArgumentException("Player not found in game state"))

  /**
   * @inheritdoc
   * @param playerId given a player, retrieve every playerId's data
   * @return the best Color to resolve trump
   * @note falls back to the first Card.Color.values
   */
  override def resolvedTrumpColor(lobbyId: LobbyId, playerId: PlayerId): Future[Card.Color] =
    onRunningPhase(lobbyId, "choose trump color")(playerId):
      case state @ GameState.ChoosingTrump(_) =>
        withHand(Some(state.core.hand)): hand =>
          engine.chooseTrumpColor(hand).getOrElse(Card.Color.values.head)

  /**
   * @inheritdoc
   * @param playerId the ID of the player requesting the bid.
   * @return A suggested [[Bid]].
   * @see [[adjustBid]] for adjest teh suggested bid to a valid bid, based on the suggested.
   * @note falls back to [[firstValidBid]].
   */
  override def placeBid(lobbyId: LobbyId, playerId: PlayerId): Future[Bid] =
    onRunningPhase(lobbyId, "place bid")(playerId):
      case state @ GameState.Bidding(_, _, _) =>
        withHand(Some(state.core.hand)): hand =>
          engine
            .placeBid(hand, state.core.trump)
            .getOrElse(firstValidBid(state.core.round, state.bids, state.core.playersIds.size))

  /**
   * @inheritdoc
   * @param playerId the ID of the player requesting the adjustment.
   * @return A valid [[Bid]] that satisfies game constraints.
   * @note falls back to [[firstValidBid]].
   */
  override def adjustBid(lobbyId: LobbyId, playerId: PlayerId): Future[Bid] =
    onRunningPhase(lobbyId, "adjust bid")(playerId):
      case state @ GameState.Bidding(_, _, _) =>
        withHand(Some(state.core.hand)): hand =>
          val rejectedBid = state.core.round - state.bids.total
          engine
            .adjustBid(hand, rejectedBid)
            .filter(_.validateBid(state.core.round, state.bids, state.core.playersIds.size).isRight)
            .getOrElse(firstValidBid(state.core.round, state.bids, state.core.playersIds.size))

  /**
   * @inheritdoc
   * @param playerId the ID of the player.
   * @return The best [[Card]] to play.
   * @note If Prolog fails or suggests a card not in `legalCards`, it falls back to
   *       playing the first legal card available.
   */
  override def bestCard(lobbyId: LobbyId, playerId: PlayerId): Future[Card] =
    onRunningPhase(lobbyId, "play best card")(playerId):
      case state @ GameState.Playing(_, _, _, _, _) =>
        withHand(Some(state.core.hand)): hand =>
          val legalCards = hand.legalCards(state.table)
          engine
            .bestPlayableCard(
              hand = hand,
              winningCard = state.table.evaluateTrick(state.core.trump),
              followingColor = state.table.followingColor,
              trump = state.core.trump,
              playerBid = state.bids(playerId),
              playerTrick = state.tricksWon(playerId)
            )
            .filter(legalCards.contains)
            .getOrElse(legalCards.head)

  private def firstValidBid(round: Round, bids: Bids, totalPlayers: Int): Bid =
    (0 to round)
      .find(_.validateBid(round, bids, totalPlayers).isRight)
      .getOrElse(0)
