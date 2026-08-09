package it.unibo.pps.wizard.application.bot.strategy

import it.unibo.pps.wizard.engine.model.core.GameAction
import it.unibo.pps.wizard.engine.model.core.GameError
import it.unibo.pps.wizard.engine.model.events.FailureEvent
import it.unibo.pps.wizard.engine.model.events.InvitationEvent
import it.unibo.pps.wizard.engine.ports.AIPort

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
 * An intelligent implementation of [[BotStrategy]] that leverages the [[AIPort]].
 *
 * This strategy delegates decision-making to the Prolog-backed engine through the
 * provided port. It is designed to play optimally based on the knowledge base
 * defined in the Prolog theory.
 */
class PrologBotStrategy(port: AIPort) extends BotStrategy:

  override def resolveInvitationEvents(invitation: InvitationEvent): Future[GameAction] =
    invitation match
      case InvitationEvent.WaitingForCard(playerId, _) =>
        port.bestCard(playerId).map(card => GameAction.PlayCard(playerId, card))

      case InvitationEvent.WaitingForBid(playerId, _) =>
        port.placeBid(playerId).map(bid => GameAction.PlaceBid(playerId, bid))

      case InvitationEvent.WaitingForTrump(playerId) =>
        port
          .resolvedTrumpColor(playerId)
          .map(color => GameAction.ResolveTrumpColor(playerId, color))

  override def resolveFailedEvents(failure: FailureEvent): Future[GameAction] = failure match
    case FailureEvent.ActionFailed(playerId, reason) =>
      reason match
        case GameError.InvalidBid =>
          port.adjustBid(playerId).map(bid => GameAction.PlaceBid(playerId, bid))

        case GameError.CardNotAllowed(notAllowedReason) =>
          Future.successful(GameAction.PlayCard(playerId, notAllowedReason.legitCards.head))

        case _ =>
          Future.failed(IllegalStateException(s"AI cannot recover from $reason"))
