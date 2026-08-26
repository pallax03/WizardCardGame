package io.github.pallax03.wizard.util

import alice.tuprolog.Prolog
import alice.tuprolog.SolveInfo
import alice.tuprolog.Term
import alice.tuprolog.Theory

import scala.jdk.CollectionConverters._

/**
 * Utility object to interface Scala with tuProlog.
 *
 * This engine wraps the imperative nature of tuProlog into a functional,
 * lazy stream of solutions, allowing for elegant backtracking within Scala
 * via [[LazyList]].
 */
object PrologEngine:

  given Conversion[String, Theory] = Theory.parseWithStandardOperators(_)

  /**
   * Builds a Prolog solver engine from a given theory.
   *
   * @param theory the Prolog theory containing rules and facts.
   * @return A function that takes a query string (goal) and returns a [[LazyList]]
   *         of all possible [[SolveInfo]] solutions, computed on-demand.
   */
  def buildEngine(theory: Theory): String => LazyList[SolveInfo] =
    val solver = Prolog()
    solver.setTheory(theory)
    goal =>
      val iterable = new Iterable[SolveInfo]:
        override def iterator: Iterator[SolveInfo] = new Iterator[SolveInfo]:
          private var solution: Option[SolveInfo] = Some(solver.solve(goal))

          override def hasNext: Boolean =
            solution.exists(current => current.isSuccess || current.hasOpenAlternatives)

          override def next(): SolveInfo =
            try solution.get
            finally
              solution =
                if solution.get.hasOpenAlternatives then Some(solver.solveNext())
                else None

      iterable.to(LazyList)

  /**
   * Extracts variable bindings from a successful Prolog solution.
   *
   * @param solution a [[SolveInfo]] resulting from a query.
   * @return A map where keys are variable names and values are the corresponding [[Term]] bindings.
   *         Returns an empty map if the solution is not successful.
   */
  def extractVars(solution: SolveInfo): Map[String, Term] =
    if solution.isSuccess then
      solution.getBindingVars.asScala
        .map(variable => variable.getName -> solution.getTerm(variable.getName))
        .toMap
    else Map.empty
