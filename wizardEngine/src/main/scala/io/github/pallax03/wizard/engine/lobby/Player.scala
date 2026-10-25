package io.github.pallax03.wizard.engine.lobby

import io.github.pallax03.wizard.engine.lobby.BotsDifficulty.Prolog
import io.github.pallax03.wizard.engine.model.basic.PlayerId

case class LobbyPlayer(lobbyId: LobbyId, playerId: PlayerId)

/**
 * Participant in a game lobby, modeling human vs bot identity and failover states.
 *
 * Identity and active control are decoupled to support distributed seat recovery:
 *  - A human participant is identified by a private `secret` token.
 *  - An automated bot is identified by an AI [[BotsDifficulty]] strategy.
 *  - When a human disconnects or accumulates AFK timeouts, their seat is delegated to an
 *    AI bot (`replaceWithABot`) without erasing their identity.
 *  - Upon reconnecting with their secret, the human reclaims active control (`returnHuman`).
 *
 * @param id unique player identifier within the lobby.
 * @param name display name of the participant.
 * @param difficulty AI strategy if the seat is currently controlled by a bot.
 * @param isOnline real-time connectivity status (WebSocket connection active).
 * @param strikes consecutive AFK turn timeouts accumulated.
 * @param secret private authentication token for seat recovery (present for human participants).
 */
case class Player(
    id: PlayerId,
    name: String,
    difficulty: Option[BotsDifficulty] = None,
    isOnline: Boolean = false,
    strikes: Int = 0,
    secret: Option[String] = None
):

  /** Returns `true` if this participant is an active human player (not substituted by a bot). */
  def isHumanPlaying: Boolean = isHuman && !isBot

  def isHuman: Boolean = secret.isDefined

  def isBot: Boolean = difficulty.isDefined

  def replaceWithABot(botDifficulty: BotsDifficulty = Prolog): Player =
    this.copy(isOnline = false, difficulty = Option(botDifficulty))

  def returnHuman: Player =
    if isHuman then this.copy(isOnline = true, difficulty = Option.empty) else this

object Player:
  def human(id: PlayerId, name: String, secret: Option[String]): Player =
    Player(id, name, None, false, 0, secret)

  def bot(id: PlayerId, difficulty: BotsDifficulty): Player =
    Player(id, s"Bot-${id.toInt}", Some(difficulty), false, 0, None)
