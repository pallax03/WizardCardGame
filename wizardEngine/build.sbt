name := sys.env.getOrElse("PROJECT_NAME", "WizardCardGame")
version := "0.1"

val scala3Version = "3.8.3"

val scalatestVersion      = "3.2.20"
val catsVersion           = "2.13.0"
val vertxVersion          = "5.1.5"
val tuPrologVersion       = "4.1.1"

ThisBuild / scalaVersion := scala3Version
ThisBuild / scalacOptions := Seq("-Wunused:all", "-Wunused:imports", "-Werror", "-language:implicitConversions")

ThisBuild / scalafixDependencies += "org.typelevel" %% "typelevel-scalafix" % "0.2.0"

ThisBuild / libraryDependencies ++= Seq(
  "org.typelevel"             %%    "cats-core"               % catsVersion,
  "org.scalatest"             %%    "scalatest"               % scalatestVersion % Test,
  "io.vertx"                   %    "vertx-core"              % vertxVersion,
  "it.unibo.alice.tuprolog"    % "2p-core"                   % tuPrologVersion
)

assembly / assemblyJarName := s"${name.value}.jar"
assembly / mainClass := Some("it.unibo.pps.wizard.Main")
assembly / assemblyMergeStrategy := {
  case x if x.endsWith("module-info.class")            => MergeStrategy.discard
  case x if x.endsWith("io.netty.versions.properties") => MergeStrategy.first
  case x                                               => (assembly / assemblyMergeStrategy).value(x)
}

lazy val root = (project in file("."))
  .settings(
    coverageExcludedPackages :=
      "<empty>;" +
        "it\\.unibo\\.pps\\.wizard\\.Main;" +
        "it\\.unibo\\.pps\\.wizard\\.application\\..*;" +
        "it\\.unibo\\.pps\\.wizard\\.engine\\.adapters\\..*;" +
        "it\\.unibo\\.pps\\.wizard\\.engine\\.events\\..*",

    semanticdbEnabled := true,
    coverageMinimumStmtTotal := 80,
    coverageFailOnMinimum := true
  )
