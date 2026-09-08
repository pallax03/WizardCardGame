package io.github.pallax03.wizard.engine.lobby

import io.github.pallax03.wizard.engine.model.basic.PlayerId

case class Player(
    id: PlayerId,
    name: String,
    difficulty: Option[BotsDifficulty] = None,
    isOnline: Boolean = false,
    secret: Option[String] = None
):
  def isHuman: Boolean = difficulty.isEmpty

object Player:
  def human(id: PlayerId, name: String, secret: Option[String]): Player =
    Player(id, name, None, false, secret)

  def bot(id: PlayerId, difficulty: BotsDifficulty): Player =
    Player(id, s"Bot-${id.toInt}", Some(difficulty), true, None)
