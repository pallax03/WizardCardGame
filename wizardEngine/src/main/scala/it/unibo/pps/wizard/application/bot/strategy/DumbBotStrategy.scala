package it.unibo.pps.wizard.application.bot.strategy

import it.unibo.pps.wizard.engine.events.FailureEvent
import it.unibo.pps.wizard.engine.events.InvitationEvent
import it.unibo.pps.wizard.engine.model.basic.bidding.Bid
import it.unibo.pps.wizard.engine.model.basic.cards.Card
import it.unibo.pps.wizard.engine.model.basic.gameplay.Round
import it.unibo.pps.wizard.engine.model.core.GameAction
import it.unibo.pps.wizard.engine.model.core.GameError

import scala.concurrent.Future
import scala.util.Random

/**
 * A simple, randomized implementation of [[BotStrategy]].
 *
 * This strategy provides basic behavior suitable for testing or low-difficulty settings.
 * Actions are chosen randomly from valid possibilities.
 */
class DumbBotStrategy(random: Random = Random()) extends BotStrategy:
  private var bid: (Round, Bid) = Round.start -> 0

  private def asyncWrapper(gameAction: GameAction): Future[GameAction] =
    Future.successful(gameAction)

  override def resolveInvitationEvents(invitation: InvitationEvent): Future[GameAction] =
    asyncWrapper:
      invitation match
        case InvitationEvent.WaitingForBid(playerId, round) =>
          bid = round -> random.nextInt(round.value + 1)
          GameAction.PlaceBid(
            playerId,
            bid._2
          )

        case InvitationEvent.WaitingForCard(playerId, legalCards) =>
          GameAction.PlayCard(playerId, legalCards.head)

        case InvitationEvent.WaitingForTrump(playerId) =>
          val colors = Card.Color.values
          GameAction.ResolveTrumpColor(playerId, colors(random.nextInt(colors.length)))

  override def resolveFailedEvents(failure: FailureEvent): Future[GameAction] =
    asyncWrapper:
      failure match
        case FailureEvent.ActionFailed(playerId, reason) =>
          reason match
            case GameError.InvalidBid =>
              val (round, lastBid) = bid
              bid = round -> (lastBid + 1) % (round.value + 1)
              GameAction.PlaceBid(playerId, bid._2)

            case GameError.CardNotAllowed(notAllowedReason) =>
              GameAction.PlayCard(playerId, notAllowedReason.legitCards.head)

            case _ =>
              throw IllegalStateException(s"Dumb bot cannot recover from $reason")
