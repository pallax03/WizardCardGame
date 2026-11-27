package io.github.pallax03.wizard.application.bot.strategy.arena

import scala.collection.mutable
import scala.concurrent.duration.*
import scala.concurrent.{Await, ExecutionContext}
import scala.util.Random

import io.github.pallax03.wizard.engine.lobby.defaultLobbyId
import io.github.pallax03.wizard.engine.model.basic.bidding.Bid
import io.github.pallax03.wizard.engine.model.basic.cards.Deck
import io.github.pallax03.wizard.engine.model.basic.gameplay.{Round, *}
import io.github.pallax03.wizard.engine.model.basic.{PlayerId, Scoreboard}
import io.github.pallax03.wizard.engine.model.core.GameEngine
import io.github.pallax03.wizard.engine.model.core.state.{ServerCoreState, ServerGameState}
import io.github.pallax03.wizard.engine.model.events.{ActionEvent, InvitationEvent, ProgressEvent}
import io.github.pallax03.wizard.engine.model.rules.RoundManager.*
import io.github.pallax03.wizard.engine.ports.InMemoryInboundPort

/** In-memory Hexagonal Self-Play Arena for deterministic duplicate-deal bot benchmarking. */
object BotSelfPlayArena:

  def playMatch(
      seats: List[ArenaSeat],
      decks: IndexedSeq[Deck],
      maxRounds: Option[Int] = None
  )(using ExecutionContext): List[SeatMatchResult] =
    require(seats.size >= 3 && seats.size <= 6, s"Wizard requires 3-6 players, got ${seats.size}")
    val totalRounds = maxRounds.getOrElse(Deck.TOTAL_SIZE / seats.size)
    require(decks.size >= totalRounds, s"Need at least $totalRounds decks, got ${decks.size}")

    val inbound = new InMemoryInboundPort
    val playerIds = (1 to seats.size).map(PlayerId.apply).toList
    val instances = playerIds.zip(seats.map(_.instantiate(inbound))).toMap
    val latencies = playerIds.map(_ -> mutable.ArrayBuffer.empty[Long]).toMap
    val exactBids = mutable.Map.from(playerIds.map(_ -> 0))
    val bidAbsErrors = mutable.Map.from(playerIds.map(_ -> 0))
    val bidBiases = mutable.Map.from(playerIds.map(_ -> 0))

    var core = ServerCoreState.initialize(playerIds, Round.start)
    var currentRound = Round.start
    var lastScoreboard = Scoreboard.empty

    for roundIdx <- 1 to totalRounds do
      val roundCore = core.copy(round = currentRound)
      val (newCore, initialState) =
        currentRound.initialize(decks(roundIdx - 1)).run(roundCore).value
      val roundBids = mutable.Map.empty[PlayerId, Bid]
      val roundTricks = mutable.Map.from(playerIds.map(_ -> 0))

      @scala.annotation.tailrec
      def stepRound(state: ServerGameState, invitationOpt: Option[InvitationEvent]): Scoreboard =
        val invitation = invitationOpt.getOrElse(
          throw new AssertionError(s"Missing invitation in round $roundIdx for state $state")
        )
        inbound.update(state)
        val pid = invitation.destinationId
        val startNs = System.nanoTime()
        val action = Await.result(
          instances(pid).strategy.resolveInvitation(defaultLobbyId, invitation),
          10.seconds
        )
        latencies(pid).addOne(System.nanoTime() - startNs)

        val flow = GameEngine.processAction(state, action) match
          case Right(f) => f
          case Left(err) =>
            throw new AssertionError(s"Illegal bot action $action in round $roundIdx: $err")

        var scored: Option[Scoreboard] = None
        flow.events.foreach:
          case ActionEvent.BidPlaced(p, b) => roundBids.update(p, b)
          case ProgressEvent.TrickWon(winner, _, _) =>
            roundTricks.update(winner, roundTricks(winner) + 1)
          case ProgressEvent.RoundScored(_, sb) => scored = Some(sb)
          case _                                => ()

        scored match
          case Some(updatedSb) => updatedSb
          case None =>
            val nextInv = flow.events.collectFirst { case inv: InvitationEvent => inv }
            stepRound(flow.state, nextInv)

      lastScoreboard = stepRound(initialState, initialState.pendingInvitation(newCore.dealerId))

      playerIds.foreach: pid =>
        val diff = roundBids(pid) - roundTricks(pid)
        if diff == 0 then exactBids.update(pid, exactBids(pid) + 1)
        bidAbsErrors.update(pid, bidAbsErrors(pid) + math.abs(diff))
        bidBiases.update(pid, bidBiases(pid) + diff)

      core = newCore.copy(scoreboard = lastScoreboard)
      currentRound = currentRound.next

    val finalScores =
      playerIds.map(pid => pid -> lastScoreboard.getStatsForRound(core.round, pid)._1).toMap
    val maxScore = finalScores.values.max
    val winnerCount = finalScores.values.count(_ == maxScore)

    playerIds
      .zip(seats)
      .map: (pid, spec) =>
        val score = finalScores(pid)
        val inst = instances(pid)
        SeatMatchResult(
          name = spec.name,
          score = score,
          winShare = if score == maxScore then 1.0 / winnerCount else 0.0,
          exactBids = exactBids(pid),
          totalRounds = totalRounds,
          bidAbsErrorSum = bidAbsErrors(pid),
          bidBiasSum = bidBiases(pid),
          aiDecisions = inst.decisions(),
          aiFallbacks = inst.fallbacks(),
          latenciesNanos = latencies(pid).toVector
        )

  def runDuplicateTournament(
      seats: List[ArenaSeat],
      deals: Int,
      seed: Long = 42L,
      maxRounds: Option[Int] = None
  )(using ExecutionContext): List[BotBenchmarkStats] =
    val rng = new Random(seed)
    val totalRounds = maxRounds.getOrElse(Deck.TOTAL_SIZE / seats.size)
    val allResults = (0 until deals).flatMap: _ =>
      val decks = IndexedSeq.fill(totalRounds)(Deck.create(rng))
      seats.indices.flatMap: rot =>
        playMatch(seats.drop(rot) ++ seats.take(rot), decks, maxRounds)
    ArenaMetrics.aggregate(allResults)

  extension (seats: List[ArenaSeat])
    def play(
        deals: Int,
        seed: Long = 42L,
        maxRounds: Option[Int] = None
    )(using ExecutionContext): List[BotBenchmarkStats] =
      runDuplicateTournament(seats, deals, seed, maxRounds)

export BotSelfPlayArena.*
