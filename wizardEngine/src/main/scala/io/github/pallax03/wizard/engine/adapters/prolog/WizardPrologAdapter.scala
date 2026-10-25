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
 * AI adapter connecting the [[io.github.pallax03.wizard.engine.ports.AIPort]] contract to the Prolog knowledge base.
 *
 * Implements a defensive safety pipeline over raw Prolog responses:
 *  1. Phase Verification: Ensures queries only execute within matching [[PlayerGameState]] phases.
 *  2. Hook Rule Compliance: If Prolog's initial bid violates dealer constraints ([[BiddingRules.validateBid]]),
 *     queries `adjustBid` to guarantee a rule-compliant bid.
 *  3. Move Legality Filtering: Filters Prolog card recommendations against [[Hand.legalCards]] to
 *     prevent illegal table plays.
 */
class WizardPrologAdapter(inboundPort: InboundPort) extends AIPort(inboundPort):

  private val engine = WizardPrologEngine()

  /** @inheritdoc */
  override protected def resolveTrumpColorLogic(
      playerId: PlayerId
  ): PartialFunction[PlayerGameState, Option[Card.Color]] =
    case state @ GameState.ChoosingTrump(_) =>
      engine.chooseTrumpColor(state.core.hand)

  /** @inheritdoc */
  override protected def placeBidLogic(
      playerId: PlayerId
  ): PartialFunction[PlayerGameState, Option[Bid]] =
    case state @ GameState.Bidding(_, _, _) =>
      val hand = state.core.hand
      engine
        .placeBid(hand, state.core.trump)
        .flatMap: bid =>
          val (round, bids, players) = (state.core.round, state.bids, state.core.playersIds.size)
          bid.validateBid(round, bids, players) match
            case Left(_) =>
              engine.adjustBid(hand, bid).filter(_.validateBid(round, bids, players).isRight)
            case Right(_) => Option(bid)

  /** @inheritdoc */
  override protected def bestCardLogic(
      playerId: PlayerId
  ): PartialFunction[PlayerGameState, Option[Card]] =
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
