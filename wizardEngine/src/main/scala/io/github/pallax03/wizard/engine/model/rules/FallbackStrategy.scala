package io.github.pallax03.wizard.engine.model.rules

import scala.util.Random

import cats.syntax.all.*

import io.github.pallax03.wizard.engine.model.basic.PlayerId
import io.github.pallax03.wizard.engine.model.basic.cards.Card
import io.github.pallax03.wizard.engine.model.core.GameAction
import io.github.pallax03.wizard.engine.model.core.state.{GameState, PlayerGameState}
import io.github.pallax03.wizard.engine.model.rules.BiddingRules.notValidBid
import io.github.pallax03.wizard.engine.model.rules.TableRules.{evaluateTrick, legalCards}

/**
 * Hand-aware legal fallback strategy shared by casual bots,
 * AI recovery, and AFK turn timeouts.
 */
object FallbackStrategy:

  private val SlipRate = 0.25
  private val BidJitter = Vector(-1, 0, 0, 1)

  /** Generates a legal [[GameAction]] for `playerId` when it is their turn in `state`. */
  def fallbackMove(state: PlayerGameState, playerId: PlayerId)(using
      rng: Random = Random
  ): Option[GameAction] = state match
    case GameState.ChoosingTrump(core) if core.dealerId == playerId =>
      val counts = core.hand.toList.foldMap:
        case Card.Standard(c, _) => Map(c -> 1)
        case _                   => Map.empty[Card.Color, Int]
      val colors = Card.Color.values.toVector
      val max = colors.map(counts.getOrElse(_, 0)).max
      GameAction
        .ResolveTrumpColor(playerId, colors.filter(counts.getOrElse(_, 0) == max).pickRandom)
        .some

    case GameState.Bidding(core, bids, turn) if turn == playerId =>
      val estimate = core.hand.toList.foldMap(cardWeight(_, core.trump.effectiveColor))
      val target = (math.round(estimate).toInt + BidJitter.pickRandom).max(0).min(core.round)
      val forbidden = bids.notValidBid(core.round, core.playersIds.size)
      val bid = (0 to core.round).filterNot(forbidden.contains).minBy(b => math.abs(b - target))
      GameAction.PlaceBid(playerId, bid).some

    case GameState.Playing(core, bids, table, turn, tricksWon) if turn == playerId =>
      val legal = core.hand.legalCards(table)
      val wantWin = tricksWon(playerId) < bids(playerId)
      val chosen =
        if legal.size == 1 then legal.head
        else if rng.nextDouble() < SlipRate then legal.pickRandom
        else if table.playedCards.nonEmpty then
          val (winners, losers) =
            legal.partition(c => (table + (playerId -> c)).evaluateTrick(core.trump).contains(c))
          val pool = if wantWin then winners else losers
          pool.toNel.fold(legal.pickRandom)(_.toList.pickRandom)
        else
          val weight = cardWeight(_, core.trump.effectiveColor)
          if wantWin then legal.maxBy(weight) else legal.minBy(weight)
      GameAction.PlayCard(playerId, chosen).some

    case _ => none

  private def cardWeight(card: Card, trump: Option[Card.Color]): Double = card match
    case c if c.isWizard                          => 1.0
    case Card.Standard(c, r) if trump.contains(c) => if r.value >= 8 then 0.7 else 0.3
    case Card.Standard(_, r) if r.value >= 11     => 0.5
    case _                                        => 0.0

  extension [A](xs: Seq[A]) private def pickRandom(using rng: Random): A = xs(rng.nextInt(xs.size))
