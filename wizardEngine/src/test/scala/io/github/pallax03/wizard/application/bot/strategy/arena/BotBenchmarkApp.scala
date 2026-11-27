package io.github.pallax03.wizard.application.bot.strategy.arena

import scala.concurrent.ExecutionContext.Implicits.global
import scala.io.Source
import scala.util.Using

import io.github.pallax03.wizard.engine.adapters.prolog.WizardPrologEngine
import io.github.pallax03.wizard.engine.model.basic.cards.Deck
import io.github.pallax03.wizard.util.PrologEngine

import alice.tuprolog.Theory

/** CLI entry point for on-demand duplicate-deal bot benchmarks (`sbt benchmark`). */
object BotBenchmarkApp:

  private final case class Config(
      preset: String = "prolog-vs-dumb",
      gitRef: Option[String] = None,
      deals: Int = 30,
      seed: Long = 42L,
      players: Int = 4,
      rounds: Option[Int] = None,
      showHelp: Boolean = false
  )

  def main(args: Array[String]): Unit =
    val cfg = parseArgs(args.toList, Config())
    if cfg.showHelp then
      println(
        """sbt "benchmark [prolog-vs-dumb|prolog-mirror|dumb-mirror|vs-git <ref>] [--deals N] [--seed L] [--players P] [--rounds R]"
          |Defaults: prolog-vs-dumb --deals 30 --seed 42 --players 4""".stripMargin
      )
    else
      val seats = cfg.preset match
        case "prolog-vs-dumb" => (prolog named "Prolog").versus(dumb, cfg.players)
        case "prolog-mirror"  => cfg.players x (prolog named "Prolog")
        case "dumb-mirror"    => cfg.players x dumb
        case "vs-git" =>
          val ref =
            cfg.gitRef.getOrElse(throw new IllegalArgumentException("vs-git requires <ref>"))
          (prolog named "Prolog(HEAD)").versus(
            prolog(s"Prolog($ref)", loadGitTheory(ref)),
            cfg.players
          )
        case other =>
          throw new IllegalArgumentException(
            s"Unknown preset: $other (run: sbt \"benchmark help\")"
          )

      val totalRounds = cfg.rounds.getOrElse(Deck.TOTAL_SIZE / cfg.players)
      val presetDesc = cfg.gitRef.fold(cfg.preset)(r => s"${cfg.preset} ($r)")
      println(
        s"=== Benchmark [$presetDesc] | ${cfg.deals} deals (${cfg.deals * cfg.players} games) | ${cfg.players}p x ${totalRounds}r | seed=${cfg.seed} ==="
      )
      println(seats.play(cfg.deals, cfg.seed, cfg.rounds).report)

  @scala.annotation.tailrec
  private def parseArgs(args: List[String], acc: Config): Config = args match
    case Nil                             => acc
    case ("help" | "--help" | "-h") :: _ => acc.copy(showHelp = true)
    case ("prolog-vs-dumb" | "prolog-mirror" | "dumb-mirror") :: tail =>
      parseArgs(tail, acc.copy(preset = args.head))
    case "vs-git" :: ref :: tail => parseArgs(tail, acc.copy(preset = "vs-git", gitRef = Some(ref)))
    case "--deals" :: n :: tail  => parseArgs(tail, acc.copy(deals = n.toInt))
    case "--seed" :: s :: tail   => parseArgs(tail, acc.copy(seed = s.toLong))
    case "--players" :: p :: tail => parseArgs(tail, acc.copy(players = p.toInt))
    case "--rounds" :: r :: tail  => parseArgs(tail, acc.copy(rounds = Some(r.toInt)))
    case unknown :: _ =>
      throw new IllegalArgumentException(
        s"Unknown argument: $unknown (run: sbt \"benchmark help\")"
      )

  private def loadGitTheory(ref: String): Theory =
    import PrologEngine.given
    WizardPrologEngine.theoryResources
      .map: res =>
        val p = new ProcessBuilder("git", "show", s"$ref:wizardEngine/src/main/resources/$res")
          .redirectErrorStream(true)
          .start()
        val out = Using.resource(Source.fromInputStream(p.getInputStream))(_.mkString)
        val exit = p.waitFor()
        require(exit == 0, s"git show failed ($exit) for $ref:$res: $out")
        out
      .mkString("\n")
