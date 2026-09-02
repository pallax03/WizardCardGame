package io.github.pallax03.wizard.application.bot.strategy

import scala.concurrent.Future
import scala.util.Random

import io.github.pallax03.wizard.engine.lobby.LobbyId
import io.github.pallax03.wizard.engine.model.basic.bidding.Bid
import io.github.pallax03.wizard.engine.model.basic.cards.Card
import io.github.pallax03.wizard.engine.model.basic.gameplay.Round
import io.github.pallax03.wizard.engine.model.basic.gameplay.Round._
import io.github.pallax03.wizard.engine.model.core.{GameAction, GameError}
import io.github.pallax03.wizard.engine.model.events.{FailureEvent, InvitationEvent}

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

  override def resolveInvitationEvents(
      lobbyId: LobbyId,
      invitation: InvitationEvent
  ): Future[GameAction] =
    asyncWrapper:
      invitation match
        case InvitationEvent.WaitingForBid(playerId, round) =>
          bid = round -> random.nextInt(round.next)
          GameAction.PlaceBid(
            playerId,
            bid._2
          )

        case InvitationEvent.WaitingForCard(playerId, legalCards) =>
          GameAction.PlayCard(playerId, legalCards.head)

        case InvitationEvent.WaitingForTrump(playerId) =>
          val colors = Card.Color.values
          GameAction.ResolveTrumpColor(playerId, colors(random.nextInt(colors.length)))

  override def resolveFailedEvents(lobbyId: LobbyId, failure: FailureEvent): Future[GameAction] =
    asyncWrapper:
      failure match
        case FailureEvent.ActionFailed(playerId, reason) =>
          reason match
            case GameError.InvalidBid =>
              val (round, lastBid) = bid
              bid = round -> (lastBid + 1) % round.next
              GameAction.PlaceBid(playerId, bid._2)

            case GameError.CardNotAllowed(notAllowedReason) =>
              GameAction.PlayCard(playerId, notAllowedReason.legitCards.head)

            case _ =>
              throw IllegalStateException(s"Dumb bot cannot recover from $reason")
