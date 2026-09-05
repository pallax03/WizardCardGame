package io.github.pallax03.wizard.engine.configuration

/**
 * Represents the configuration of a game.
 *
 * @param timer in seconds The custom timer for each player's action.
 */
case class GameConfiguration(
    timer: Int = 60
)
