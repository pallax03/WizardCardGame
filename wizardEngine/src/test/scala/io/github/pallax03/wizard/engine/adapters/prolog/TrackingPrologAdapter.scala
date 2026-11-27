package io.github.pallax03.wizard.engine.adapters.prolog

import java.util.concurrent.atomic.AtomicInteger

import io.github.pallax03.wizard.engine.model.basic.PlayerId
import io.github.pallax03.wizard.engine.model.basic.bidding.Bid
import io.github.pallax03.wizard.engine.model.basic.cards.Card
import io.github.pallax03.wizard.engine.model.core.state.PlayerGameState
import io.github.pallax03.wizard.engine.ports.InboundPort

/** [[WizardPrologAdapter]] decorator tracking total AI decisions and fallback (`None`) occurrences. */
final class TrackingPrologAdapter(
    inboundPort: InboundPort,
    engine: WizardPrologEngine = WizardPrologEngine()
) extends WizardPrologAdapter(inboundPort, engine):
  private val _decisions = AtomicInteger(0)
  private val _fallbacks = AtomicInteger(0)

  def decisions: Int = _decisions.get()
  def fallbacks: Int = _fallbacks.get()

  private def track[A](res: Option[A]): Option[A] =
    _decisions.incrementAndGet()
    if res.isEmpty then _fallbacks.incrementAndGet()
    res

  override protected def resolveTrumpColorLogic(
      playerId: PlayerId
  ): PartialFunction[PlayerGameState, Option[Card.Color]] =
    super.resolveTrumpColorLogic(playerId).andThen(track)

  override protected def placeBidLogic(
      playerId: PlayerId
  ): PartialFunction[PlayerGameState, Option[Bid]] =
    super.placeBidLogic(playerId).andThen(track)

  override protected def bestCardLogic(
      playerId: PlayerId
  ): PartialFunction[PlayerGameState, Option[Card]] =
    super.bestCardLogic(playerId).andThen(track)
