name := sys.env.getOrElse("PROJECT_NAME", "WizardCardGame")
version := "0.1"

val scala3Version = "3.8.3"

val scalatestVersion      = "3.2.20"
val catsVersion           = "2.13.0"
val vertxVersion          = "5.1.6"
val tuPrologVersion       = "4.1.1"
val circeVersion          = "0.14.16"
val logbackVersion        = "1.6.3"
val tapirVersion          = "1.13.31"
val tapirCirceVersion     = "3.11.0"

ThisBuild / scalaVersion := scala3Version
ThisBuild / scalacOptions := Seq("-Wunused:all", "-Wunused:imports", "-Werror", "-language:implicitConversions", "-Wconf:msg=not declared infix:s")
Test / scalacOptions += "-Wconf:msg=Alphanumeric method:s"

ThisBuild / scalafixDependencies += "org.typelevel" %% "typelevel-scalafix" % "0.2.0"

ThisBuild / libraryDependencies ++= Seq(
  "org.typelevel"             %%    "cats-core"               % catsVersion,
  "org.scalatest"             %%    "scalatest"               % scalatestVersion % Test,
  "io.vertx"                   %    "vertx-core"              % vertxVersion,
  "io.vertx"                   %    "vertx-web"               % vertxVersion,
  "io.vertx"                   %    "vertx-redis-client"      % vertxVersion,
  "it.unibo.alice.tuprolog"    %    "2p-core"                 % tuPrologVersion,
  "io.circe"                  %%    "circe-core"              % circeVersion,
  "io.circe"                  %%    "circe-generic"           % circeVersion,
  "io.circe"                  %%    "circe-parser"            % circeVersion,
  "ch.qos.logback"             %    "logback-classic"         % logbackVersion,
  "com.softwaremill.sttp.tapir" %%    "tapir-core"            % tapirVersion,
  "com.softwaremill.sttp.tapir" %%    "tapir-vertx-server"    % tapirVersion,
  "com.softwaremill.sttp.tapir" %%    "tapir-json-circe"      % tapirVersion,
  "com.softwaremill.sttp.tapir" %% "tapir-swagger-ui-bundle"  % tapirVersion,
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
        "it\\.unibo\\.pps\\.wizard\\.engine\\.ports\\..*;" +
        "it\\.unibo\\.pps\\.wizard\\.util\\..*",

    semanticdbEnabled := true,
    coverageMinimumStmtTotal := 75,
    coverageFailOnMinimum := true
  )
