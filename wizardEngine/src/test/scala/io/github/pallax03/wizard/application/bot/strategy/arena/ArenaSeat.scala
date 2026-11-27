package io.github.pallax03.wizard.application.bot.strategy.arena

import io.github.pallax03.wizard.application.bot.strategy.{
  BotStrategy,
  DumbBotStrategy,
  PrologBotStrategy
}
import io.github.pallax03.wizard.engine.adapters.prolog.{TrackingPrologAdapter, WizardPrologEngine}
import io.github.pallax03.wizard.engine.ports.InboundPort

import alice.tuprolog.Theory

final case class SeatInstance(
    strategy: BotStrategy,
    decisions: () => Int = () => 0,
    fallbacks: () => Int = () => 0
)

final case class ArenaSeat(name: String, instantiate: InboundPort => SeatInstance)

object ArenaSeat:
  val dumbName = "DumbBot"
  def dumb: ArenaSeat = ArenaSeat(dumbName, _ => SeatInstance(new DumbBotStrategy))
  val prologName = "PrologBot"
  def prolog: ArenaSeat = prolog(prologName)

  def prolog(name: String, theory: Theory = WizardPrologEngine.defaultTheory): ArenaSeat =
    ArenaSeat(
      name,
      inbound =>
        val adapter = new TrackingPrologAdapter(inbound, WizardPrologEngine(theory))
        SeatInstance(
          new PrologBotStrategy(adapter),
          () => adapter.decisions,
          () => adapter.fallbacks
        )
    )

  extension (seat: ArenaSeat)
    infix def named(label: String): ArenaSeat = seat.copy(name = label)
    infix def -(other: ArenaSeat): List[ArenaSeat] = List(seat, other)
    infix def vs(other: ArenaSeat): List[ArenaSeat] = List(seat, other, seat, other)
    def versus(other: ArenaSeat, players: Int): List[ArenaSeat] =
      (0 until players).toList.map(i => if i % 2 == 0 then seat else other)

  extension (count: Int) infix def x(seat: ArenaSeat): List[ArenaSeat] = List.fill(count)(seat)
  extension (seats: List[ArenaSeat]) infix def -(other: ArenaSeat): List[ArenaSeat] = seats :+ other

export ArenaSeat.*
