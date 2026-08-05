package it.unibo.pps.wizard.engine.model.core

import it.unibo.pps.wizard.engine.model.basic._
import it.unibo.pps.wizard.engine.model.basic.cards.Hands
import it.unibo.pps.wizard.engine.model.basic.gameplay.Round
import it.unibo.pps.wizard.engine.model.basic.gameplay.Trump
import it.unibo.pps.wizard.engine.model.rules.RoundManager.firstPlayer

/**
 * Holds the immutable state of the game.
 *
 * @param players    the collection of players participating in the game.
 * @param hands      the current cards held by each player.
 * @param trump      the trump determined for the current round.
 * @param round      the current round.
 * @param dealerId   the ID of the player currently acting as the dealer.
 * @param scoreboard the accumulated scores of all players across rounds.
 */
case class CoreState(
    playersIds: List[PlayerId],
    hands: Hands,
    trump: Trump,
    round: Round,
    dealerId: PlayerId,
    scoreboard: Scoreboard
):
  /** Returns a new [[CoreState]] with the updated trump. */
  def updateTrump(trump: Trump): CoreState = this.copy(trump = trump)

object CoreState:

  /**
   * Initializes a new core instance with default values, for the round specified.
   *
   * @param players total participants, players and bots.
   * @param round   usually used for [[Round.start]].
   * @return A starting [[CoreState]] with empty hands, Absent trump, and empty scoreboard.
   */
  def initialize(
      playersIds: List[PlayerId],
      round: Round
  ): CoreState =
    CoreState(
      playersIds = playersIds,
      hands = Hands.empty,
      trump = Trump.Absent,
      round = round,
      dealerId = round.firstPlayer(playersIds),
      scoreboard = Scoreboard.empty
    )
