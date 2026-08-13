package it.unibo.pps.wizard.engine.lobby

import it.unibo.pps.wizard.engine.model.basic.PlayerId

case class Player(id: PlayerId, name: String, difficulty: Option[BotsDifficulty] = None)