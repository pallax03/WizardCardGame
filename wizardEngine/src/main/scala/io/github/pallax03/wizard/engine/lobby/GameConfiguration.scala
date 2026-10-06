package io.github.pallax03.wizard.engine.lobby

/**
 * Table bounds and timing constants for Wizard game sessions.
 *
 * According to official rules, a Wizard table requires between 3 and 6 players.
 */
object GameConfiguration:
  val MIN_PLAYERS: Int = 3
  val MAX_PLAYERS: Int = 6
  private val gracePeriodSeconds: Int = 3

/** Validation errors for configuration parameters outside acceptable limits. */
enum ConfigurationErrors(val min: Int, val max: Int):
  /** Turn timer outside the allowed range of `[15, 90]` seconds. */
  case TimerNotValid extends ConfigurationErrors(15, 90)

  /** Maximum strikes outside the allowed range of `[1, 3]` timeouts. */
  case MaxStrikesNotValid extends ConfigurationErrors(1, 3)

/**
 * Game session configuration defining per-turn timeouts and AFK strike thresholds.
 *
 * @param timer turn timeout in seconds allocated per action; must be within `[15, 90]` seconds (default: 30).
 * @param maxStrikes consecutive AFK timeouts before a player is flagged or substituted; must be within `[1, 3]` strikes (default: 2).
 */
case class GameConfiguration(
    timer: Int = 30,
    maxStrikes: Int = 2
):
  /**
   * Validates parameters against allowed ranges:
   *  - `timer` ∈ `[15, 90]` seconds
   *  - `maxStrikes` ∈ `[1, 3]` strikes
   *
   * @return `Right(())` if valid, or a [[ConfigurationErrors]] variant on violation.
   */
  def validate: Either[ConfigurationErrors, Unit] =
    for
      _ <- Either.cond(
        timer >= ConfigurationErrors.TimerNotValid.min && timer <= ConfigurationErrors.TimerNotValid.max,
        (),
        ConfigurationErrors.TimerNotValid
      )
      _ <- Either.cond(
        maxStrikes >= ConfigurationErrors.MaxStrikesNotValid.min && maxStrikes <= ConfigurationErrors.MaxStrikesNotValid.max,
        (),
        ConfigurationErrors.MaxStrikesNotValid
      )
    yield ()

  /**
   * Calculates the effective turn TTL in seconds using exponential halving:
   * `max(1, (timer + gracePeriod) / 2^strikes)`.
   *
   * Each consecutive AFK strike halves the allocated action window to prevent inactive
   * players from blocking the game, while granting a 3-second grace period on the baseline.
   */
  def calculateTTL(strikes: Int): Int =
    Math.max(1, (timer + GameConfiguration.gracePeriodSeconds) / Math.pow(2, strikes).toInt)
