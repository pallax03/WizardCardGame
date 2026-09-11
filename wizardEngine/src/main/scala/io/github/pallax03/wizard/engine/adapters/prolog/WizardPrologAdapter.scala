package io.github.pallax03.wizard.engine.adapters.prolog

import io.github.pallax03.wizard.engine.adapters.prolog.WizardPrologEngine
import io.github.pallax03.wizard.engine.model.basic.PlayerId
import io.github.pallax03.wizard.engine.model.basic.bidding.{Bid, Bids}
import io.github.pallax03.wizard.engine.model.basic.cards.Card
import io.github.pallax03.wizard.engine.model.basic.gameplay.Table
import io.github.pallax03.wizard.engine.model.core.state.{GameState, PlayerGameState}
import io.github.pallax03.wizard.engine.model.rules.BiddingRules.*
import io.github.pallax03.wizard.engine.model.rules.TableRules.*
import io.github.pallax03.wizard.engine.ports.{AIPort, InboundPort}

/**
 * Adapter that connects the game engine's AI requirements with the Prolog knowledge base.
 *
 * This Adapter work with [[GameEngineInboundAdapter]] as every api need to get actual state to respond with the correct data for the correct playerId request.
 *
 * This component acts as a safety layer:
 * 1. Validates that the AI requests are performed during the correct game phases.
 * 2. Manages interactions with the [[WizardPrologEngine]].
 * 3. Provides robust fallbacks: if Prolog fails to return a valid move, this adapter ensures the game continues by providing a valid default move.
 */
class WizardPrologAdapter(inboundPort: InboundPort) extends AIPort(inboundPort):

  private val engine = WizardPrologEngine()

  /** @inheritdoc */
  override protected def resolveTrumpColorLogic(playerId: PlayerId): PartialFunction[PlayerGameState, Option[Card.Color]] =
    case state @ GameState.ChoosingTrump(_) =>
      engine.chooseTrumpColor(state.core.hand)

  /** @inheritdoc  */
  override protected def placeBidLogic(playerId: PlayerId): PartialFunction[PlayerGameState, Option[Bid]] =
    case state @ GameState.Bidding(_, _, _) =>
      val hand = state.core.hand
      engine.placeBid(hand, state.core.trump).flatMap: bid =>
        val (round, bids, players) = (state.core.round, state.bids, state.core.playersIds.size)
        bid.validateBid(round, bids, players) match
          case Left(_) => engine.adjustBid(hand, bid).filter(_.validateBid(round, bids, players).isRight)
          case Right(_) => Option(bid)
  
  /** @inheritdoc */
  override protected def bestCardLogic(playerId: PlayerId): PartialFunction[PlayerGameState, Option[Card]] =
    case state @ GameState.Playing(_, _, _, _, _) =>
      val hand = state.core.hand
      engine
        .bestPlayableCard(
          hand = hand,
          winningCard = state.table.evaluateTrick(state.core.trump),
          followingColor = state.table.followingColor,
          trump = state.core.trump,
          playerBid = state.bids(playerId),
          playerTrick = state.tricksWon(playerId)
        )
        .filter(hand.legalCards(state.table).contains)
