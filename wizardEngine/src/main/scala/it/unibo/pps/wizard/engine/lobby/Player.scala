package it.unibo.pps.wizard.engine.lobby

import it.unibo.pps.wizard.engine.model.basic.PlayerId

case class Player(id: PlayerId, name: String, bot: Option[BotsDifficulty] = None)

object Player:
  def human(id: PlayerId, name: String): Player = Player(id, name, Option.empty)
  def bot(id: PlayerId, difficulty: BotsDifficulty): Player =
    Player(id, s"Bot-$difficulty-${id.toInt}", Some(difficulty))