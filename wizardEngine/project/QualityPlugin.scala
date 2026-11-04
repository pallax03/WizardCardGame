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
      Seq(
        scalacOptions := Seq(
          "-deprecation",
          "-feature",
          "-Wunused:all",
          "-Werror",
          "-language:implicitConversions",
          "-Wconf:msg=not declared infix:s"
        )
      )

  override def projectSettings: Seq[Def.Setting[?]] = Seq(
    Compile / packageDoc / publishArtifact := false,
    semanticdbEnabled := true,
    coverageMinimumStmtTotal := 75,
    coverageFailOnMinimum := true,
    coverageExcludedPackages := Seq(
      s"$BasePkg.Main",
      s"$BasePkg.application.*",
      s"$BasePkg.engine.adapters.*",
      s"$BasePkg.engine.ports.*",
      s"$BasePkg.engine.configuration.*",
      s"$BasePkg.engine.lobby.*",
      s"$BasePkg.util.*"
    ).mkString(";")
  )
}
