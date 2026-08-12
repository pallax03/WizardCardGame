package it.unibo.pps.wizard.engine.lobby

type Player = (name: String, bot: Option[BotsDifficulty])

object Player:
  def human(name: String): Player = (name, Option.empty)
  def bot(botsDifficulty: BotsDifficulty): Player = ("", Option(botsDifficulty))