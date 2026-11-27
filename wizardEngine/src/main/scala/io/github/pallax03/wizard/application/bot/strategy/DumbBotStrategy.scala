package io.github.pallax03.wizard.application.bot.strategy

import scala.concurrent.Future

import io.github.pallax03.wizard.engine.lobby.LobbyId
import io.github.pallax03.wizard.engine.model.basic.bidding.Bid
import io.github.pallax03.wizard.engine.model.basic.cards.Card
import io.github.pallax03.wizard.engine.model.events.InvitationEvent
import io.github.pallax03.wizard.engine.model.events.InvitationEvent.{WaitingForBid, WaitingForCard}
import io.github.pallax03.wizard.engine.ports.InboundPort

/**
 * Casual bot strategy that delegates move decisions to [[FallbackStrategy]]
 * via the template method in [[BotStrategy]].
 */
class DumbBotStrategy(inboundPort: InboundPort) extends BotStrategy(inboundPort):
  private val recoveredByBotStrategy = Future.failed(Exception("recovered with FallbackStrategy"))
  override protected def chooseCard(lobbyId: LobbyId, event: WaitingForCard): Future[Card] =
    recoveredByBotStrategy

  override protected def chooseBid(lobbyId: LobbyId, event: WaitingForBid): Future[Bid] =
    recoveredByBotStrategy

  override protected def chooseTrump(
      lobbyId: LobbyId,
      event: InvitationEvent.WaitingForTrump
  ): Future[Card.Color] = recoveredByBotStrategy
