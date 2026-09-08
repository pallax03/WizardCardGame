package io.github.pallax03.wizard.engine.lobby

import io.github.pallax03.wizard.engine.lobby.BotsDifficulty.Prolog
import io.github.pallax03.wizard.engine.model.basic.PlayerId

case class LobbyPlayer(lobbyId: LobbyId, playerId: PlayerId)

case class Player(
    id: PlayerId,
    name: String,
    difficulty: Option[BotsDifficulty] = None,
    isOnline: Boolean = false,
    secret: Option[String] = None
):

  /**
   * A human player can be replaced with a bot
   * @return True if the human player is playing, False is ambiguous
   */
  def isHumanPlaying: Boolean = isHuman && !isBot

  /** @return True if is a human, can be replaced with a bot so check [[isHumanPlaying]] */
  def isHuman: Boolean = secret.isDefined

  /** @return True if is controlled by the [[BotManagerVerticle]] */
  def isBot: Boolean = difficulty.isDefined

  def replaceWithABot(botDifficulty: BotsDifficulty = Prolog): Player =
    this.copy(isOnline = false, difficulty = Option(botDifficulty))
  def returnHuman: Player = if isHuman then this.copy(isOnline = true, difficulty = Option.empty)
  else throw IllegalCallerException("Player is not a human")

object Player:
  def human(id: PlayerId, name: String, secret: Option[String]): Player =
    Player(id, name, None, false, secret)

  def bot(id: PlayerId, difficulty: BotsDifficulty): Player =
    Player(id, s"Bot-${id.toInt}", Some(difficulty), false, None)
