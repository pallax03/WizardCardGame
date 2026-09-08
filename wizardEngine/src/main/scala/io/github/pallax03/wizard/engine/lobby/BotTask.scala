package io.github.pallax03.wizard.engine.lobby

import io.github.pallax03.wizard.engine.model.events.WizardEvent

/**
 * Payload published to the Redis Stream `bot:tasks` for each bot turn.
 *
 * @param lobbyId    the lobby where the bot is playing.
 * @param invitation the typed [[WizardEvent]] (always an [[io.github.pallax03.wizard.engine.model.events.InvitationEvent]]).
 */
case class BotTask(lobbyId: LobbyId, invitation: WizardEvent)

