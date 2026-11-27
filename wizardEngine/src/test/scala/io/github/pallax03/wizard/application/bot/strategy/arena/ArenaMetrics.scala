package io.github.pallax03.wizard.application.bot.strategy.arena

final case class SeatMatchResult(
    name: String,
    score: Int,
    winShare: Double, // 1.0 / winnerCount (splits ties)
    exactBids: Int,
    totalRounds: Int,
    bidAbsErrorSum: Int,
    bidBiasSum: Int, // signed (bid - tricks): + overbid, - underbid
    aiDecisions: Int,
    aiFallbacks: Int,
    latenciesNanos: Vector[Long]
)

final case class BotBenchmarkStats(
    name: String,
    gamesPlayed: Int,
    winRate: Double, // per-seat win share %
    avgScore: Double,
    exactBidRate: Double,
    bidMae: Double,
    bidBias: Double, // + overbid, - underbid
    aiDecisions: Int,
    aiFallbacks: Int,
    fallbackRate: Option[Double], // None for non-AI bots (e.g. DumbBot)
    avgLatencyMs: Double,
    p95LatencyMs: Double
)

object ArenaMetrics:
  def aggregate(results: Seq[SeatMatchResult]): List[BotBenchmarkStats] =
    results
      .groupBy(_.name)
      .toList
      .map: (name, entries) =>
        val n = entries.size
        val totalRounds = entries.map(_.totalRounds).sum.toDouble
        val totalDecisions = entries.map(_.aiDecisions).sum
        val totalFallbacks = entries.map(_.aiFallbacks).sum
        val allLatencies = entries.flatMap(_.latenciesNanos).sorted
        val avgLatMs =
          if allLatencies.isEmpty then 0.0
          else (allLatencies.sum.toDouble / allLatencies.size) / 1e6
        val p95LatMs =
          if allLatencies.isEmpty then 0.0
          else allLatencies(((allLatencies.size - 1) * 0.95).toInt) / 1e6

        BotBenchmarkStats(
          name = name,
          gamesPlayed = n,
          winRate = (entries.map(_.winShare).sum / n) * 100.0,
          avgScore = entries.map(_.score.toDouble).sum / n,
          exactBidRate = (entries.map(_.exactBids).sum / totalRounds) * 100.0,
          bidMae = entries.map(_.bidAbsErrorSum).sum / totalRounds,
          bidBias = entries.map(_.bidBiasSum).sum / totalRounds,
          aiDecisions = totalDecisions,
          aiFallbacks = totalFallbacks,
          fallbackRate =
            if totalDecisions == 0 then None
            else Some((totalFallbacks.toDouble / totalDecisions) * 100.0),
          avgLatencyMs = avgLatMs,
          p95LatencyMs = p95LatMs
        )
      .sortBy(-_.avgScore)

  def formatReport(stats: List[BotBenchmarkStats]): String =
    val header =
      f"${"Bot"}%-16s | ${"Games"}%5s | ${"Win%/s"}%6s | ${"AvgScore"}%8s | ${"ExactBid%"}%9s | ${"MAE"}%5s | ${"Bias"}%6s | ${"Fallback%"}%9s | ${"AvgMs"}%6s | ${"P95Ms"}%6s"
    val sep = "-" * header.length
    val rows = stats.map: s =>
      val fbStr = s.fallbackRate.fold("N/A")(r => f"$r%8.2f%%")
      f"${s.name}%-16s | ${s.gamesPlayed}%5d | ${s.winRate}%5.1f%% | ${s.avgScore}%8.1f | ${s.exactBidRate}%8.1f%% | ${s.bidMae}%5.2f | ${s.bidBias}%+6.2f | $fbStr%9s | ${s.avgLatencyMs}%6.2f | ${s.p95LatencyMs}%6.2f"
    (header +: sep +: rows).mkString("\n")

  extension (stats: List[BotBenchmarkStats]) def report: String = formatReport(stats)

export ArenaMetrics.*
