package it.unibo.pps.wizard.engine.model.basic.scoreboard

import it.unibo.pps.wizard.engine.model.basic.PlayerId
import it.unibo.pps.wizard.engine.model.basic.Players
import it.unibo.pps.wizard.engine.model.basic.bidding.Bid
import it.unibo.pps.wizard.engine.model.basic.gameplay.Round

/**
 * Represents a row of statistics for a specific round, mapping each player to their
 * optional score and bid.
 *
 * @param round the game round these statistics refer to.
 * @param playerStats a map linking each player to their optional score and bid for this round.
 */
case class RoundRow(round: Round, playerStats: Map[PlayerId, Option[(Score, Bid)]]):
  /**
   * Returns the score of the player as a string.
   *
   * @param pId the ID of the player.
   * @return the score string, or an empty string if not present.
   */
  def getScore(pId: PlayerId): String =
    playerStats.get(pId).flatten.map(data => data._1.toString).getOrElse("")

  /**
   * Returns the bid of the player as a string.
   *
   * @param pId the ID of the player.
   * @return the bid string, or an empty string if not present.
   */
  def getBid(pId: PlayerId): String =
    playerStats.get(pId).flatten.map(data => data._2.toString).getOrElse("")

object RoundRow:
  private def calculateMaxRounds(numPlayers: Int): Int = 60 / numPlayers

  /**
   * Retrieves the score and bid stats for all players in a specific round.
   *
   * @param round   the target game round.
   * @param players the list of players.
   * @param sb      the scoreboard containing game history.
   * @return a map of player IDs to their optional round stats.
   */
  def getStatsForAllPlayers(
      round: Round,
      players: Players,
      sb: Scoreboard
  ): Map[PlayerId, Option[(Score, Bid)]] =
    players.toList.map { p =>
      val playerHistory = sb(p.id)
      p.id -> playerHistory.get(round)
    }.toMap

  /**
   * Generates and updates all round rows up to the maximum playable rounds.
   *
   * @param players the list of players.
   * @param sb      the current scoreboard.
   * @return a list of updated [[RoundRow]]s.
   */
  def updateRows(players: Players, sb: Scoreboard): List[RoundRow] =
    val maxRounds = calculateMaxRounds(players.toList.size)

    (1 to maxRounds).map { rNum =>
      val round = Round(rNum)
      RoundRow(round, getStatsForAllPlayers(round, players, sb))
    }.toList

  /**
   * Initializes all round rows with empty statistics for each player.
   *
   * @param players the list of players.
   * @return a list of empty [[RoundRow]]s for the entire game.
   */
  def initRows(players: Players): List[RoundRow] =
    val maxRounds = calculateMaxRounds(players.toList.size)

    (1 to maxRounds).map { rNum =>
      val round = Round(rNum)
      RoundRow(round, players.toList.map(p => p.id -> None).toMap)
    }.toList
