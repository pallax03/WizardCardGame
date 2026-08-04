package it.unibo.pps.wizard.engine.adapters.prolog

import alice.tuprolog.Term
import alice.tuprolog.Theory
import it.unibo.pps.wizard.engine.model.basic.bidding.Bid
import it.unibo.pps.wizard.engine.model.basic.bidding.Trick
import it.unibo.pps.wizard.engine.model.basic.cards.Card
import it.unibo.pps.wizard.engine.model.basic.cards.Hand
import it.unibo.pps.wizard.engine.model.basic.gameplay.Trump
import WizardTermMapper._
import it.unibo.pps.wizard.util.PrologEngine

import scala.util.Using

/**
 * An adapter engine that delegates AI decision-making to a Prolog-based knowledge base.
 *
 * Every api is built to give always a correct and a valid value,
 * but prolog theory can be overwritten, so Option act as a fallback.
 *
 * Fallback need to be handled.
 *
 * This component acts as a bridge:
 * 1. Serializes Scala domain models into Prolog terms using [[WizardTermMapper]].
 * 2. Executes queries against the loaded `prolog/all.pl` theory.
 * 3. Deserializes the resulting Prolog terms back into Scala types (e.g., [[Bid]], [[Card]]).
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

  /**
   * Uses Prolog logic to determine the best trump color to choose.
   *
   * @param hand The player's current hand.
   *
   * @return The chosen [[Card.Color]] if the Prolog engine succeeds.
   */
  def chooseTrumpColor(hand: Hand): Option[Card.Color] =
    query(s"choose_trump(${cardsTerm(hand.toList)}, TrumpColor)", "TrumpColor").flatMap(term =>
      Card.Color.values.find(colorTerm(_) == term.toString)
    )

  /**
   * Queries Prolog to determine the initial bid based on hand strength and trump.
   *
   * @param hand    The player's current hand.
   * @param trump   The current Trump of the round.
   *
   * @return A [[Bid]] instance representing the suggested bid.
   */
  def placeBid(hand: Hand, trump: Trump): Option[Bid] =
    query(s"place_bid(${cardsTerm(hand.toList)}, ${trumpColorTerm(trump)}, Bid)", "Bid").map(term =>
      term.toString.toInt
    )

  /**
   * Queries Prolog to adjust a previously rejected bid.
   *
   * @param hand           The player's current hand.
   * @param rejectedBid    player's rejectedBid (if is not a real rejected won the new Bid can be not right).
   *
   * @return A suggested [[Bid]] that conforms to game constraints.
   */
  def adjustBid(hand: Hand, rejectedBid: Bid): Option[Bid] =
    query(s"adjust_bid(${cardsTerm(hand.toList)}, $rejectedBid, FinalBid)", "FinalBid").map(term =>
      term.toString.toInt
    )

  /**
   * Evaluates the best playable card from the hand according to Prolog's strategy rules.
   *
   * @param hand           The player's current hand.
   * @param winningCard    The current strongest card on the table, if any.
   * @param followingColor The color currently required by the trick, if any.
   * @param trump          The current trump for the round.
   * @param playerBid      The player's bid.
   * @param playerTrick    The number of tricks already won by the player.
   * @return The best card to play.
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
