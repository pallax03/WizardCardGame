package it.unibo.pps.wizard.engine.model.configuration

import it.unibo.pps.wizard.engine.model.lobby.LobbyPlayer

/**
 * Represents the configuration of a game.
 *
 * @param timer The custom timer for each player's action.
 * @param players List of players (human and bots).
 */
case class GameConfiguration(
    timer: Long, // todo: to be implemented during heartbeat pattern issue#3
    players: List[LobbyPlayer]
)
