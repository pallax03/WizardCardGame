package it.unibo.pps.wizard.engine.model.configuration

/** Represents the configuration of a game, including the player's name, the number of bots, and the difficulty level of the bots. */
enum BotsDifficulty:
  case Dumb
  case Prolog

/**
 * Represents the configuration of a game, including the player's name, the number of bots, and the difficulty level of the bots.
 *
 * @param playerName The name of the human player.
 * @param numberOfBots The number of bot players in the game.
 * @param botsDifficulty The difficulty level of the bot players.
 */
case class GameConfiguration(
    playerName: String,
    numberOfBots: Int,
    botsDifficulty: BotsDifficulty
)
