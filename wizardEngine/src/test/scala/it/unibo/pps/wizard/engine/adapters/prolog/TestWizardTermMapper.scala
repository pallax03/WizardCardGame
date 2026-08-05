package it.unibo.pps.wizard.engine.adapters.prolog

import it.unibo.pps.wizard.engine.model.basic._
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class TestWizardTermMapper extends AnyWordSpec with Matchers:

  import cards.Card.*
  import gameplay.Trump
  "WizardTermMapper" when:
    import WizardTermMapper.*
    "mapping color to term" should:
      "a valid color" in:
        colorTerm(Blue) shouldBe "blue"
        colorTerm(Red) shouldBe "red"

      "not valid color" in:
        colorTerm(Option.empty) shouldBe NO_VALUE

    "mapping trump to term" should:
      "Absent Trump No color" in:
        val trump = Trump.Absent
        trumpColorTerm(trump) shouldBe NO_VALUE
      "Standard Trump have same color of the card" in:
        val trump = Option(Five of Red).asTrump
        trumpColorTerm(trump) shouldBe "red"

    "mapping card to term" should:
      "not valid card" in:
        cardTerm(Option.empty) shouldBe NO_VALUE
      "a special card" in:
        cardTerm(jester) shouldBe "jester"
        cardTerm(wizard) shouldBe "wizard"
      "a standard card" in:
        cardTerm(Five of Red) shouldBe "card(5,red)"

    "mapping cards to term" should:
      val cards = (Five of Red) - wizard - jester
      "a list of valid cards" in:
        cardsTerm(cards) shouldBe "[card(5,red),wizard,jester]"
