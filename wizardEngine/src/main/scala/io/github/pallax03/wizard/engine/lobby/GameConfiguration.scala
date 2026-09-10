package io.github.pallax03.wizard.engine.lobby

object GameConfiguration:
  val MIN_PLAYERS: Int = 3
  val MAX_PLAYERS: Int = 6
  private val gracePeriodSeconds: Int = 3

enum ConfigurationErrors(val min: Int, val max: Int):
  case TimerNotValid extends ConfigurationErrors(30, 120)
  case MaxStrikesNotValid extends ConfigurationErrors(1, 3)

/**
 * Represents the configuration of a game.
 *
 * @param timer in seconds: the maximum time each player has to perform an action per turn.
 * @param maxStrikes number of consecutive AFK timeouts before a player is automatically disconnected.
 */
case class GameConfiguration(
    timer: Int = 60,
    maxStrikes: Int = 2
):
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
    
  def calculateTTL(strikes: Int): Int = Math.max(1, (timer + GameConfiguration.gracePeriodSeconds) / Math.pow(2, strikes).toInt) 