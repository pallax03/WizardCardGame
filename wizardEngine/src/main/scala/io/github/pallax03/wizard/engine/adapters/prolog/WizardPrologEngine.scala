package io.github.pallax03.wizard.engine.adapters.prolog

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
class WizardPrologEngine(theory: Theory = WizardPrologEngine.defaultTheory):
  private val prologEngine = PrologEngine.buildEngine(theory)

  /** Queries Prolog goal `choose_trump/2` to select the color maximizing hand potential. */
  def chooseTrumpColor(hand: Hand): Option[Card.Color] =
    query(s"choose_trump(${cardsTerm(hand.toList)}, TrumpColor)", "TrumpColor").flatMap(term =>
      Card.Color.values.find(colorTerm(_) == term.toString)
    )

  /** Queries Prolog goal `place_bid/6` (falling back to `place_bid/3`) for contextual bidding. */
  def placeBid(
      hand: Hand,
      trump: Trump,
      totalPlayers: Int = 4,
      bidsPlaced: Int = 0,
      otherBidsSum: Int = 0
  ): Option[Bid] =
    val h = cardsTerm(hand.toList)
    val t = trumpColorTerm(trump)
    query(s"place_bid($h, $t, $totalPlayers, $bidsPlaced, $otherBidsSum, Bid)", "Bid")
      .orElse(query(s"place_bid($h, $t, Bid)", "Bid"))
      .map(_.toString.toInt)

  /** Queries Prolog goal `adjust_bid/7` (falling back to `adjust_bid/3`) for Hook Rule recovery. */
  def adjustBid(
      hand: Hand,
      rejectedBid: Bid,
      trump: Trump = Trump.Absent,
      totalPlayers: Int = 4,
      bidsPlaced: Int = 0,
      otherBidsSum: Int = 0
  ): Option[Bid] =
    val h = cardsTerm(hand.toList)
    val t = trumpColorTerm(trump)
    query(
      s"adjust_bid($h, $t, $totalPlayers, $bidsPlaced, $otherBidsSum, $rejectedBid, FinalBid)",
      "FinalBid"
    ).orElse(query(s"adjust_bid($h, $rejectedBid, FinalBid)", "FinalBid"))
      .map(_.toString.toInt)

  /** Queries Prolog goal `best_playable_card/10` (falling back to `/7`) for contextual card play. */
  def bestPlayableCard(
      hand: Hand,
      winningCard: Option[Card],
      followingColor: Option[Card.Color],
      trump: Trump,
      playerBid: Bid,
      playerTrick: Trick,
      tableCards: List[Card] = Nil,
      remainingPlayers: Int = 0,
      tableOverbid: Int = 0
  ): Option[Card] =
    val h = cardsTerm(hand.toList)
    val tbl = cardsTerm(tableCards)
    val w = cardTerm(winningCard)
    val f = colorTerm(followingColor)
    val t = trumpColorTerm(trump)
    query(
      s"best_playable_card($h, $tbl, $w, $f, $t, $playerBid, $playerTrick, $remainingPlayers, $tableOverbid, BestCard)",
      "BestCard"
    ).orElse(
      query(
        s"best_playable_card($h, $w, $f, $t, $playerBid, $playerTrick, BestCard)",
        "BestCard"
      )
    ).flatMap(term => hand.toList.find(cardTerm(_) == term.toString))

  /** Helper to execute a goal and extract a specific variable from the solution. */
  private def query[B](goal: String, extractTerm: String): Option[Term] =
    prologEngine(goal)
      .find(_.isSuccess)
      .flatMap(solution => PrologEngine.extractVars(solution).get(extractTerm))

object WizardPrologEngine:
  val theoryResources: Seq[String] = Seq(
    "prolog/utils.pl",
    "prolog/domain.pl",
    "prolog/rules.pl",
    "prolog/strategy.pl",
    "prolog/api.pl"
  )

  lazy val defaultTheory: Theory = PrologEngine.loadFromResources(theoryResources)
