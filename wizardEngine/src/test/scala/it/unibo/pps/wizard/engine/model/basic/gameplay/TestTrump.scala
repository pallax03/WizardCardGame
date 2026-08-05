package it.unibo.pps.wizard.engine.model.basic.gameplay

import it.unibo.pps.wizard.engine.model.basic._
import it.unibo.pps.wizard.engine.model.core.GameError

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import scala.language.postfixOps

class TestTrump extends AnyWordSpec with Matchers:

  import cards.Card
  import cards.Card.*
  import gameplay.Trump
  "A Trump" when:
    "Absent" should:
      val trump = Trump.Absent
      "provide no card and no effective color" in:
        trump.card shouldBe None
        trump.effectiveColor shouldBe None
        trump resolveWizard Blue match
          case Left(error) => error shouldBe GameError.InvalidAction
          case _           =>

    "created from a Standard card" should:
      val trump = Trump(Five of Red)
      "extract color and card natively" in:
        trump shouldBe a[Trump.Standard]
        trump.effectiveColor shouldBe Some(Red)
        trump.card.get shouldBe (Five of Red)

    "created from a Jester" should:
      val j = jester
      val trump = Trump(j)
      "behave like Absent but remember the physical card" in:
        trump shouldBe a[Trump.Jester]
        trump.effectiveColor shouldBe None
        trump.card shouldBe Some(j)

    "created from a Wizard" should:
      val w = wizard
      val trump = Trump(w)
      "initialized as Unresolved" in:
        trump shouldBe a[Trump.WizardUnresolved]
        trump.effectiveColor shouldBe None
        trump.card shouldBe Some(w)

      "Unresolved to Resolved" in:
        trump resolveWizard Blue match
          case Right(resolvedTrump) =>
            resolvedTrump shouldBe a[Trump.WizardResolved]
            resolvedTrump.effectiveColor shouldBe Some(Blue)
            resolvedTrump.card shouldBe Some(w)
          case _ =>

    "from Option" should:
      "Absent if Option is empty" in:
        val trump = Option.empty.asTrump
        trump shouldBe Trump.Absent

      "Standard if Option is a Trump.Standard" in:
        val trump = Option(Five of Red).asTrump
        trump shouldBe a[Trump.Standard]

      "Jester if Option is a Trump.Jester" in:
        val trump = Option(jester).asTrump
        trump shouldBe a[Trump.Jester]

      "Wizard if Option is a Trump.WizardUnresolved" in:
        val trump = Option(wizard).asTrump
        trump shouldBe a[Trump.WizardUnresolved]
