package it.unibo.pps.wizard.engine.lobby

import it.unibo.pps.wizard.engine.model.basic.PlayerId

/**
 * Represents a player inside a pre-game lobby.
 * This is an application-level entity used for matchmaking and network routing,
 * distinct from the pure game logic's PlayerId.
 *
 * @param id the unique identifier of the player in the game logic scope.
 * @param name the display name of the player.
 * @param bot empty if the player is not controlled by `application.bot`.
 */
case class LobbyPlayer(id: PlayerId, name: String, bot: Option[BotsDifficulty])
