package it.unibo.pps.wizard.engine.model.rules

import it.unibo.pps.wizard.engine.model.basic._
import it.unibo.pps.wizard.engine.model.basic.cards._
import it.unibo.pps.wizard.engine.model.basic.gameplay._
import it.unibo.pps.wizard.engine.model.core.CoreState
import it.unibo.pps.wizard.engine.model.core.GameError
import it.unibo.pps.wizard.engine.model.core.GameState
import org.scalatest.OptionValues.convertOptionToValuable
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class TestRoundManager extends AnyWordSpec with Matchers:
  import Round.*

  val p1: PlayerId = PlayerId(1)
  val p2: PlayerId = PlayerId(2)
  val p3: PlayerId = PlayerId(3)

  val playersIds: List[PlayerId] = List(p1, p2, p3)
  
  "RoundManager on round 1" when:
    "managing turn order" should:
      "find the next player correctly" in:
        playersIds.nextAfter(p1) shouldBe Right(p2)
        playersIds.nextAfter(p3) shouldBe Right(p1)

      "fail if current player is not in the list" in:
        playersIds.nextAfter(PlayerId(99)).shouldBe(Left(GameError.NotYourTurn))

    "determining the first player of a round" should:
      "rotate correctly based on the round number" in:
        val round = Round.start
        round.firstPlayer(playersIds).shouldBe(p1)
        round.next.firstPlayer(playersIds).shouldBe(p2)
        round.next.next.firstPlayer(playersIds).shouldBe(p3)
        round.next.next.next.firstPlayer(playersIds).shouldBe(p1)

    "dealing cards" should:
      "distribute the correct amount of cards based on the round" in:
        val initialDeck = Deck.create
        val round = Round.start
        val (deckAfter, (hands, trump)) = round.deal(playersIds).run(initialDeck).value

        hands.getHand(p1).toList.length.shouldBe(1)
        hands.getHand(p2).toList.length.shouldBe(1)
        trump.isDefined.shouldBe(true)
        deckAfter.length.shouldBe(Deck.TOTAL_SIZE - 3 - 1)

      "handle deals where no cards are left for the trump card" in:
        val initialDeck = Deck.create
        val maxRound = (1 until 20).foldLeft(Round.start)((r, _) => r.next)

        val (deckAfter, (hands, trump)) = maxRound.deal(playersIds).run(initialDeck).value

        hands.getHand(p1).value.toList.length.shouldBe(20)
        trump.isEmpty.shouldBe(true)
        deckAfter.length.shouldBe(0)
        maxRound.isLastRound(playersIds).shouldBe(true)

      "popped trump should not be in deck or in any player's hand" in:
        val initialDeck = Deck.create
        val round = Round.start.next.next.next.next.next.next
        val (deckAfter, (hands, trump)) = round.deal(playersIds).run(initialDeck).value

        trump.foreach { t =>
          deckAfter.cards.contains(t).shouldBe(false)
          playersIds.toList.foreach { player =>
            hands.getHand(player).value.toList.contains(t).shouldBe(false)
          }
        }

    "validating the turn of a player" should:
      "succeed if the action player matches the expected player" in:
        val expected = p2
        expected.validateTurnOf(p2).shouldBe(Right(()))

      "fail with NotYourTurn if the action player is different" in:
        val expected = p2
        expected.validateTurnOf(p1).shouldBe(Left(GameError.NotYourTurn))

    "initializing a new round" should:
      import Card.*
      val deckCards = (One of Red) - (Two of Yellow) - jester
      "correctly transition to Bidding state" in:
        val round = Round.start
        val Card_TrumpResolved = Thirteen of Green
        val TrumpResolved = Option(Card_TrumpResolved).asTrump
        val customDeck_TrumpResolved = Deck.create(deckCards - Card_TrumpResolved)

        round
          .initialize(customDeck_TrumpResolved)
          .runA(CoreState.initialize(playersIds, round))
          .value match
          case biddingState: GameState.Bidding =>
            biddingState.core.hands.getHand(p1).value.toList.length.shouldBe(1)
            biddingState.currentPlayer.shouldBe(p1)
            biddingState.core.trump.shouldBe(TrumpResolved)
          case _ => ()

      "correctly transition to ChoosingTrump state" in:
        val round = Round.start
        val Card_TrumpUnResolved = wizard
        val TrumpUnResolved = Option(Card_TrumpUnResolved).asTrump
        val customDeck_TrumpUnresolved = Deck.create(deckCards - Card_TrumpUnResolved)

        round
          .initialize(customDeck_TrumpUnresolved)
          .runA(CoreState.initialize(playersIds, round))
          .value match
          case choosingState: GameState.ChoosingTrump =>
            choosingState.core.hands.getHand(p1).value.toList.length.shouldBe(1)
            choosingState.core.trump.shouldBe(TrumpUnResolved)
          case _ => ()
