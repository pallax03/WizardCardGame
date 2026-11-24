import sbt.*

object Dependencies {
  val scala3 = "3.9.0"

  private val vertx = "5.1.8"
  private val circe = "0.14.17"
  private val tapir = "1.14.1"
  private val apispec = "0.12.1"

  val libraries: Seq[ModuleID] = Seq(
    "org.typelevel" %% "cats-core" % "2.13.0",
    "it.unibo.alice.tuprolog" % "2p-core" % "4.1.1",
    "io.vertx" % "vertx-core" % vertx,
    "io.vertx" % "vertx-web" % vertx,
    "io.vertx" % "vertx-redis-client" % vertx,
    "io.circe" %% "circe-core" % circe,
    "io.circe" %% "circe-generic" % circe,
    "io.circe" %% "circe-parser" % circe,
    "com.softwaremill.sttp.tapir" %% "tapir-core" % tapir,
    "com.softwaremill.sttp.tapir" %% "tapir-vertx-server" % tapir,
    "com.softwaremill.sttp.tapir" %% "tapir-json-circe" % tapir,
    "com.softwaremill.sttp.tapir" %% "tapir-swagger-ui-bundle" % tapir,
    "com.softwaremill.sttp.tapir" %% "tapir-asyncapi-docs" % tapir,
    "com.softwaremill.sttp.apispec" %% "asyncapi-circe-yaml" % apispec,
    "com.softwaremill.sttp.tapir" %% "tapir-prometheus-metrics" % tapir,
    "ch.qos.logback" % "logback-classic" % "1.6.8",
    "net.logstash.logback" % "logstash-logback-encoder" % "9.0",
    "org.scalatest" %% "scalatest" % "3.2.20" % Test,
    "com.tngtech.archunit" % "archunit" % "1.5.1" % Test,
    "com.softwaremill.sttp.tapir" %% "tapir-sttp-stub4-server" % tapir % Test
  )
}
