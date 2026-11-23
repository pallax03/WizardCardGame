package io.github.pallax03.wizard.engine

import com.tngtech.archunit.core.importer.{ClassFileImporter, ImportOption}
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class TestHexagonalArchitecture extends AnyWordSpec with Matchers:

  private val basePkg = "io.github.pallax03.wizard"
  private val ioPackages = Array(
    "io.vertx..",
    "io.circe..",
    "sttp.tapir..",
    "sttp.apispec..",
    "alice.tuprolog.."
  )

  private val productionClasses = ClassFileImporter()
    .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
    .importPackages(basePkg)

  private def assertNoDependenciesOn(sourcePkg: String, forbiddenPkgs: String*): Unit =
    noClasses()
      .that()
      .resideInAPackage(sourcePkg)
      .should()
      .dependOnClassesThat()
      .resideInAnyPackage(forbiddenPkgs*)
      .check(productionClasses)

  "Hexagonal architecture" when:
    "checking engine.model" should:
      "not depend on outer hexagonal layers" in:
        assertNoDependenciesOn(
          s"$basePkg.engine.model..",
          s"$basePkg.engine.ports..",
          s"$basePkg.engine.adapters..",
          s"$basePkg.engine.lobby..",
          s"$basePkg.application..",
          s"$basePkg.codecs..",
          s"$basePkg.util.."
        )

      "not depend on I/O, HTTP, or serialization libraries" in:
        assertNoDependenciesOn(s"$basePkg.engine.model..", ioPackages*)

    "checking engine.lobby" should:
      "remain decoupled from adapters, codecs, and I/O frameworks" in:
        assertNoDependenciesOn(
          s"$basePkg.engine.lobby..",
          Seq(
            s"$basePkg.engine.adapters..",
            s"$basePkg.application..",
            s"$basePkg.codecs..",
            s"$basePkg.util.."
          ) ++ ioPackages*
        )

    "checking engine.ports" should:
      "remain independent from adapters, codecs, and transport implementations" in:
        assertNoDependenciesOn(
          s"$basePkg.engine.ports..",
          s"$basePkg.engine.adapters..",
          s"$basePkg.application..",
          s"$basePkg.codecs..",
          "io.vertx.redis..",
          "io.circe..",
          "sttp.tapir..",
          "alice.tuprolog.."
        )
