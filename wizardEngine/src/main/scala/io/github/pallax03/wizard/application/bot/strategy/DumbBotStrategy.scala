package io.github.pallax03.wizard.application.bot.strategy

import scala.concurrent.Future
import io.github.pallax03.wizard.engine.lobby.LobbyId
import io.github.pallax03.wizard.engine.model.core.{GameAction, GameError}
import io.github.pallax03.wizard.engine.model.events.InvitationEvent.{WaitingForBid, WaitingForCard}
import io.github.pallax03.wizard.engine.model.events.{FailureEvent, InvitationEvent}
import io.github.pallax03.wizard.engine.model.rules.FallbackStrategy

/**
 * A simple, randomized implementation of [[BotStrategy]].
 *
 * This strategy provides basic behavior suitable for testing or low-difficulty settings.
 * Actions are chosen randomly from valid possibilities.
 */
class DumbBotStrategy extends BotStrategy:

  override def resolveInvitationEvents(
      lobbyId: LobbyId,
      invitation: InvitationEvent
  ): Future[GameAction] =
    Future.successful(FallbackStrategy.fallbackMove(invitation))

  override def resolveFailedEvents(lobbyId: LobbyId, failure: FailureEvent): Future[GameAction] =
    Future.successful:
      failure match
        case FailureEvent.ActionFailed(playerId, reason) =>
          reason match
            case GameError.InvalidBid(round, invalidBid) =>
              FallbackStrategy.fallbackMove(WaitingForBid(playerId, round, Option(invalidBid)))
              
            case GameError.CardNotAllowed(notAllowedReason) =>
              FallbackStrategy.fallbackMove(WaitingForCard(playerId, notAllowedReason.legitCards))

            case _ =>
              throw IllegalStateException(s"Dumb bot cannot recover from $reason")
