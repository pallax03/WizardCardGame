package io.github.pallax03.wizard.engine.lobby

import io.github.pallax03.wizard.engine.model.basic.PlayerId

case class Player(
    id: PlayerId,
    name: String,
    difficulty: Option[BotsDifficulty] = None,
    isOnline: Boolean = false,
    secret: Option[String] = None
)
