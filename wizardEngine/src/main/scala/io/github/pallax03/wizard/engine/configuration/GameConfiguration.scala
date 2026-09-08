package io.github.pallax03.wizard.engine.configuration

/**
 * Represents the configuration of a game.
 *
 * @param timer in seconds: the maximum time each player has to perform an action per turn.
 * @param gracePeriodSeconds extra seconds added on top of timer to account for network latency before triggering timeout.
 * @param maxStrikes number of consecutive AFK timeouts before a player is automatically disconnected.
 */
case class GameConfiguration(
    timer: Int = 60,
    gracePeriodSeconds: Int = 3,
    maxStrikes: Int = 2
)
