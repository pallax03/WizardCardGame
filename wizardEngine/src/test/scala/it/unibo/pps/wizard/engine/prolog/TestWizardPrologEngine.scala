package it.unibo.pps.wizard.engine.prolog

import it.unibo.pps.wizard.engine.model.basic.BasicTestDSL._
import it.unibo.pps.wizard.engine.model.basic.bidding.Bid
import it.unibo.pps.wizard.engine.model.basic.bidding.Trick
import it.unibo.pps.wizard.engine.model.basic.cards.Card._
import it.unibo.pps.wizard.engine.model.basic.cards.Hand._
import it.unibo.pps.wizard.engine.model.basic.gameplay.Table
import it.unibo.pps.wizard.engine.model.rules.TableRules._
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class TestWizardPrologEngine extends AnyWordSpec with Matchers:

  "WizardPrologEngine" should:
    val engine = WizardPrologEngine()
    val hand = ((One of Red) - jester - wizard - (Twelve of Yellow)).asHand
    "choosing trump color" in:
      val trumpColor = engine.chooseTrumpColor(hand).head
      Color.values should contain(trumpColor)

    "place bid" in:
      val bid = engine.placeBid(hand, Option(One of Yellow).asTrump).head
      bid should be >= 0
      bid should be <= hand.toList.size

    "adjust bid" in:
      val handSize = hand.toList.size
      val bid = engine.adjustBid(hand, Bid(handSize + 1)).head
      bid should be >= 0
      bid should be <= handSize

    "best playable card" in:
      val legalCards = hand.legalCards(Table.empty)
      val bestCard = engine
        .bestPlayableCard(
          hand = hand,
          winningCard = Option(Ten of Yellow),
          followingColor = Option(Yellow),
          trump = Option(Five of Blue).asTrump,
          playerBid = Bid(5),
          playerTrick = Trick(3)
        )
        .head
      legalCards should contain(bestCard)
