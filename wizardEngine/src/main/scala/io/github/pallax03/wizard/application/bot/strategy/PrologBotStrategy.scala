package io.github.pallax03.wizard.application.bot.strategy

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

import io.github.pallax03.wizard.engine.lobby.LobbyId
import io.github.pallax03.wizard.engine.model.core.{GameAction, GameError}
import io.github.pallax03.wizard.engine.model.events.{FailureEvent, InvitationEvent}
import io.github.pallax03.wizard.engine.model.rules.FallbackStrategy
import io.github.pallax03.wizard.engine.ports.AIPort

/**
 * An intelligent implementation of [[BotStrategy]] that leverages the [[AIPort]].
 *
 * This strategy delegates decision-making to the Prolog-backed engine through the
 * provided port. It is designed to play optimally based on the knowledge base
 * defined in the Prolog theory.
 */
class PrologBotStrategy(port: AIPort) extends BotStrategy:

  override def resolveInvitationEvents(
      lobbyId: LobbyId,
      invitation: InvitationEvent
  ): Future[GameAction] =
    invitation match
      case InvitationEvent.WaitingForCard(playerId, _) =>
        port.bestCard(lobbyId, playerId).map(card => GameAction.PlayCard(playerId, card))
          .recover { _ => FallbackStrategy.fallbackMove(invitation) }

      case InvitationEvent.WaitingForBid(playerId, _, _) =>
        port.placeBid(lobbyId, playerId).map(bid => GameAction.PlaceBid(playerId, bid))
          .recover { _ => FallbackStrategy.fallbackMove(invitation) }

      case InvitationEvent.WaitingForTrump(playerId) =>
        port
          .resolvedTrumpColor(lobbyId, playerId)
          .map(color => GameAction.ResolveTrumpColor(playerId, color))
          .recover { _ => FallbackStrategy.fallbackMove(invitation) }

  override def resolveFailedEvents(lobbyId: LobbyId, failure: FailureEvent): Future[GameAction] =
    failure match
      case FailureEvent.ActionFailed(playerId, reason) =>
        reason match
          case GameError.InvalidBid(round, invalidBid) =>
            port.adjustBid(lobbyId, playerId).map(bid => GameAction.PlaceBid(playerId, bid))
              .recover { _ => FallbackStrategy.fallbackMove(InvitationEvent.WaitingForBid(playerId, round, Option(invalidBid))) }

          case GameError.CardNotAllowed(notAllowedReason) =>
            Future.successful(GameAction.PlayCard(playerId, notAllowedReason.legitCards.head))

          case _ =>
            Future.failed(IllegalStateException(s"AI cannot recover from $reason"))
