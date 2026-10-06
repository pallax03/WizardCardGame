package io.github.pallax03.wizard.engine.model.basic

import io.github.pallax03.wizard.engine.model.basic.PlayerId
import io.github.pallax03.wizard.engine.model.basic.bidding.Bid
import io.github.pallax03.wizard.engine.model.basic.gameplay.Round

/**
 * Represents the cumulative points accumulated by a player.
 *
 * In Wizard, scores can be positive, zero, or negative depending on prediction accuracy.
 *
 * @see [[io.github.pallax03.wizard.engine.model.rules.ScoringRules]] for scoring calculations.
 */
type Score = Int

/**
 * Tracks the historical record of cumulative scores and bids for all players across game rounds.
 *
 * Maps each [[PlayerId]] to an inner map of [[Round]] to their corresponding cumulative [[Score]] and [[Bid]].
 * Implemented as an opaque type over `Map[PlayerId, Map[Round, (Score, Bid)]]`.
 */
opaque type Scoreboard = Map[PlayerId, Map[Round, (Score, Bid)]]

object Scoreboard:
  def empty: Scoreboard = Map.empty
  def apply(map: Map[PlayerId, Map[Round, (Score, Bid)]]): Scoreboard = map

  extension (sb: Scoreboard)
    def apply(pId: PlayerId): Map[Round, (Score, Bid)] = sb.getOrElse(pId, Map.empty)

    def addScore(pId: PlayerId, round: Round, points: Score, bid: Bid): Scoreboard =
      sb.updated(pId, sb.getOrElse(pId, Map.empty).updated(round, (points, bid)))

    def getStatsForRound(r: Round, pId: PlayerId): (Score, Bid) =
      sb.getOrElse(pId, Map.empty).getOrElse(r, (0, 0))

    def toMap: Map[PlayerId, Map[Round, (Score, Bid)]] = sb
