package io.github.pallax03.wizard.engine.adapters.prolog

import scala.util.Using

import io.github.pallax03.wizard.engine.adapters.prolog.WizardTermMapper.*
import io.github.pallax03.wizard.engine.model.basic.bidding.{Bid, Trick}
import io.github.pallax03.wizard.engine.model.basic.cards.{Card, Hand}
import io.github.pallax03.wizard.engine.model.basic.gameplay.Trump
import io.github.pallax03.wizard.util.PrologEngine

import alice.tuprolog.{Term, Theory}

/**
 * Low-level Prolog integration engine delegating bot reasoning to a 2P-Kt knowledge base.
 *
 * Loads modular Prolog theories (`domain.pl`, `rules.pl`, `strategy.pl`, `api.pl`)
 * and executes declarative queries, mapping solutions back into typed domain models.
 * Returns `Option` to enable robust fallback handling when Prolog queries fail.
 */
class WizardPrologEngine:
  private val prologEngine = PrologEngine.buildEngine(defineTheory)

  private def defineTheory: Theory =
    import PrologEngine.given
    Seq(
      "prolog/utils.pl",
      "prolog/domain.pl",
      "prolog/rules.pl",
      "prolog/strategy.pl",
      "prolog/api.pl"
    ).map(f => Using.resource(scala.io.Source.fromResource(f))(_.mkString)).mkString("\n")

  /** Queries Prolog goal `choose_trump/2` to select the color maximizing hand potential. */
  def chooseTrumpColor(hand: Hand): Option[Card.Color] =
    query(s"choose_trump(${cardsTerm(hand.toList)}, TrumpColor)", "TrumpColor").flatMap(term =>
      Card.Color.values.find(colorTerm(_) == term.toString)
    )

  /** Queries Prolog goal `place_bid/3` to estimate trick potential from hand strength and trump. */
  def placeBid(hand: Hand, trump: Trump): Option[Bid] =
    query(s"place_bid(${cardsTerm(hand.toList)}, ${trumpColorTerm(trump)}, Bid)", "Bid").map(term =>
      term.toString.toInt
    )

  /**
   * Queries Prolog goal `adjust_bid/3` to compute a strategic alternative when a bid
   * violates dealer Hook Rule constraints.
   */
  def adjustBid(hand: Hand, rejectedBid: Bid): Option[Bid] =
    query(s"adjust_bid(${cardsTerm(hand.toList)}, $rejectedBid, FinalBid)", "FinalBid").map(term =>
      term.toString.toInt
    )

  /**
   * Queries Prolog goal `best_playable_card/7` to evaluate the optimal legal card.
   *
   * Analyzes trick deficit vs target bid to decide between winning or dodging the trick.
   */
  def bestPlayableCard(
      hand: Hand,
      winningCard: Option[Card],
      followingColor: Option[Card.Color],
      trump: Trump,
      playerBid: Bid,
      playerTrick: Trick
  ): Option[Card] = query(
    s"""best_playable_card(
             |${cardsTerm(hand.toList)},
             |${cardTerm(winningCard)},
             |${colorTerm(followingColor)},
             |${trumpColorTerm(trump)},
             |$playerBid,
             |$playerTrick,
             |BestCard
             |)""".stripMargin,
    "BestCard"
  ).flatMap(term => hand.toList.find(cardTerm(_) == term.toString))

  /** Helper to execute a goal and extract a specific variable from the solution. */
  private def query[B](goal: String, extractTerm: String): Option[Term] =
    prologEngine(goal)
      .find(_.isSuccess)
      .flatMap(solution => PrologEngine.extractVars(solution).get(extractTerm))
