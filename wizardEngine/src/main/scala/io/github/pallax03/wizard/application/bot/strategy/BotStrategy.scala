package io.github.pallax03.wizard.application.bot.strategy

import scala.concurrent.{ExecutionContext, Future}

import io.github.pallax03.wizard.engine.lobby.{BotsDifficulty, LobbyId}
import io.github.pallax03.wizard.engine.model.basic.bidding.Bid
import io.github.pallax03.wizard.engine.model.basic.cards.Card
import io.github.pallax03.wizard.engine.model.core.GameAction
import io.github.pallax03.wizard.engine.model.events.InvitationEvent
import io.github.pallax03.wizard.engine.model.rules.FallbackStrategy
import io.github.pallax03.wizard.engine.ports.AIPort

trait BotStrategy:
  protected def chooseCard(lobbyId: LobbyId, event: InvitationEvent.WaitingForCard): Future[Card]
  protected def chooseBid(lobbyId: LobbyId, event: InvitationEvent.WaitingForBid): Future[Bid]
  protected def chooseTrump(
      lobbyId: LobbyId,
      event: InvitationEvent.WaitingForTrump
  ): Future[Card.Color]

  final def resolveInvitation(lobbyId: LobbyId, invitation: InvitationEvent)(using
      ExecutionContext
  ): Future[GameAction] =
    val actionFuture = invitation match
      case e: InvitationEvent.WaitingForCard =>
        chooseCard(lobbyId, e).map(GameAction.PlayCard(e.destinationId, _))
      case e: InvitationEvent.WaitingForBid =>
        chooseBid(lobbyId, e).map(GameAction.PlaceBid(e.destinationId, _))
      case e: InvitationEvent.WaitingForTrump =>
        chooseTrump(lobbyId, e).map(GameAction.ResolveTrumpColor(e.destinationId, _))

    actionFuture.recover { _ => FallbackStrategy.fallbackMove(invitation) }

object BotStrategy:
  def apply(difficulty: BotsDifficulty, port: AIPort): BotStrategy = difficulty match
    case BotsDifficulty.Dumb   => new DumbBotStrategy
    case BotsDifficulty.Prolog => new PrologBotStrategy(port)
