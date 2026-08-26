package io.github.pallax03.wizard.util

import io.github.pallax03.wizard.engine.model.basic.cards.Card.Color
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class TestPrologEngine extends AnyWordSpec with Matchers:

  "A Prolog Engine" should:
    import PrologEngine.given
    "Resolve a basic query with member" in:
      val engine = PrologEngine.buildEngine("")
      val solutions = engine("member(2, [1, 2, 3])")
      solutions.headOption.exists(_.isSuccess) shouldBe true
      val failedSolutions = engine("member(4, [1, 2, 3])")
      failedSolutions.headOption.exists(_.isSuccess) shouldBe false
      failedSolutions.take(1).map(PrologEngine.extractVars).toList shouldBe List()

    "Extract variables" should:
      "return an empty map if the solution is a failure" in:
        import alice.tuprolog.Prolog
        val solver = Prolog()
        val failedSolveInfo = solver.solve("fail.")
        PrologEngine.extractVars(failedSolveInfo) shouldBe Map.empty

    "Resolve Backtracking" in:
      val theory = """
          color(red).
          color(blue).
          color(green).
          color(yellow).
      """
      val engine = PrologEngine.buildEngine(theory)
      val solutions = engine("color(C)")
      val results = solutions.take(4).map(PrologEngine.extractVars).toList
      results.map(_("C").toString) should contain theSameElementsAs Color.values
        .map(_.toString.toLowerCase)
        .toList
