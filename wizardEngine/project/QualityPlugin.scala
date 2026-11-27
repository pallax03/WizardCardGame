import sbt.*
import sbt.Keys.*
import scalafix.sbt.ScalafixPlugin
import scoverage.ScoverageKeys.*
import scoverage.ScoverageSbtPlugin

object QualityPlugin extends AutoPlugin {
  override def requires: Plugins = ScalafixPlugin && ScoverageSbtPlugin

  private val BasePkg = "io.github.pallax03.wizard"

  override def buildSettings: Seq[Def.Setting[?]] =
    addCommandAlias(
      "check",
      "scalafmtCheckAll; scalafixAll --check; coverage; test; coverageReport"
    ) ++
      addCommandAlias("format", "scalafmtAll; scalafmtSbt; scalafixAll") ++
      addCommandAlias(
        "benchmark",
        "Test / runMain io.github.pallax03.wizard.application.bot.strategy.arena.BotBenchmarkApp"
      ) ++
      Seq(
        scalacOptions := Seq(
          "-deprecation",
          "-feature",
          "-Wunused:all",
          "-Werror",
          "-language:implicitConversions",
          "-Wconf:msg=not declared infix:s",
          "-Wconf:msg=Skipping coverage instrumentation:s"
        )
      )

  override def projectSettings: Seq[Def.Setting[?]] = Seq(
    Compile / packageDoc / publishArtifact := false,
    semanticdbEnabled := true,
    coverageMinimumStmtTotal := 75,
    coverageFailOnMinimum := true,
    coverageExcludedPackages := Seq(
      s"$BasePkg.Main",
      s"$BasePkg.application.bot.BotManagerVerticle.*",
      s"$BasePkg.application.timer.*",
      s"$BasePkg.application.web.ws.*",
      s"$BasePkg.application.web.http.(HttpServerVerticle|JoinLobbyRequest|PublicPlayerInfo|LobbyStateResponse|AuthLobbyPlayer).*",
      s"$BasePkg.engine.adapters.redis.*",
      s"$BasePkg.engine.adapters.VertxWebSocketsAdapter.*",
      s"$BasePkg.engine.ports.PubSubPort.*",
      s"$BasePkg.util.*"
    ).mkString(";")
  )
}
