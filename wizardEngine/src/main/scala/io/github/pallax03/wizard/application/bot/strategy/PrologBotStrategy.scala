package io.github.pallax03.wizard.application.bot.strategy

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

import io.github.pallax03.wizard.engine.lobby.LobbyId
import io.github.pallax03.wizard.engine.model.basic.bidding.Bid
import io.github.pallax03.wizard.engine.model.basic.cards.Card
import io.github.pallax03.wizard.engine.model.events.InvitationEvent
import io.github.pallax03.wizard.engine.ports.AIPort

class PrologBotStrategy(port: AIPort) extends BotStrategy:

  private def unwrap[T](f: Future[Either[?, Option[T]]]): Future[T] =
    f.flatMap:
      case Right(Some(v)) => Future.successful(v)
      case Right(None)    => Future.failed(new Exception("No AI move"))
      case Left(err)      => Future.failed(new Exception(err.toString))

  override protected def chooseCard(l: LobbyId, e: InvitationEvent.WaitingForCard): Future[Card] =
    unwrap(port.bestCard(l, e.destinationId))
  override protected def chooseBid(l: LobbyId, e: InvitationEvent.WaitingForBid): Future[Bid] =
    unwrap(port.placeBid(l, e.destinationId))
  override protected def chooseTrump(
      l: LobbyId,
      e: InvitationEvent.WaitingForTrump
  ): Future[Card.Color] = unwrap(port.resolvedTrumpColor(l, e.destinationId))
