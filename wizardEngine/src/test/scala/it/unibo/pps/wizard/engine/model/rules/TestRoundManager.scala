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

  val p1: Player = Player.human(PlayerId(1), PlayerName("Alice"))
  val p2: Player = Player.human(PlayerId(2), PlayerName("Bob"))
  val p3: Player = Player.human(PlayerId(3), PlayerName("Charlie"))

  val players: Players = Players(p1, p2, p3)

  "RoundManager on round 1" when:
    "managing turn order" should:
      "find the next player correctly" in:
        players.nextAfter(p1.id) shouldBe Right(p2.id)
        players.nextAfter(p3.id) shouldBe Right(p1.id)

      "fail if current player is not in the list" in:
        players.nextAfter(PlayerId(99)) shouldBe Left(GameError.NotYourTurn)

    "determining the first player of a round" should:
      "rotate correctly based on the round number" in:
        val round = Round.start
        round.firstPlayer(players) shouldBe p1.id
        round.next.firstPlayer(players) shouldBe p2.id
        round.next.next.firstPlayer(players) shouldBe p3.id
        round.next.next.next.firstPlayer(players) shouldBe p1.id

    "dealing cards" should:
      "distribute the correct amount of cards based on the round" in:
        val initialDeck = Deck.create
        val round = Round.start
        val (deckAfter, (hands, trump)) = round.deal(players).run(initialDeck).value

        hands.getHand(p1.id).toList should have size 1
        hands.getHand(p2.id).toList should have size 1
        trump shouldBe defined
        deckAfter.length shouldBe (Deck.TOTAL_SIZE - 3 - 1)

      "handle deals where no cards are left for the trump card" in:
        val initialDeck = Deck.create
        val maxRound = (1 until 20).foldLeft(Round.start)((r, _) => r.next)

        val (deckAfter, (hands, trump)) = maxRound.deal(players).run(initialDeck).value

        hands.getHand(p1.id).value.toList should have size 20
        trump shouldBe empty
        deckAfter.length shouldBe 0
        maxRound.isLastRound(players) shouldBe true

      "popped trump should not be in deck or in any player's hand" in:
        val initialDeck = Deck.create
        val round = Round.start.next.next.next.next.next.next
        val (deckAfter, (hands, trump)) = round.deal(players).run(initialDeck).value

        trump.foreach { t =>
          deckAfter.cards should not contain t
          players.toList.foreach { player =>
            hands.getHand(player.id).value.toList should not contain t
          }
        }

    "validating the turn of a player" should:
      "succeed if the action player matches the expected player" in:
        val expected = p2.id
        expected.validateTurnOf(p2.id) shouldBe Right(())

      "fail with NotYourTurn if the action player is different" in:
        val expected = p2.id
        expected.validateTurnOf(p1.id) shouldBe Left(GameError.NotYourTurn)

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
          .runA(CoreState.initialize(players, round))
          .value match
          case biddingState: GameState.Bidding =>
            biddingState.core.hands.getHand(p1.id).value.toList should have size 1
            biddingState.currentPlayer shouldBe p1.id
            biddingState.core.trump shouldBe TrumpResolved
          case _ => ()

      "correctly transition to ChoosingTrump state" in:
        val round = Round.start
        val Card_TrumpUnResolved = wizard
        val TrumpUnResolved = Option(Card_TrumpUnResolved).asTrump
        val customDeck_TrumpUnresolved = Deck.create(deckCards - Card_TrumpUnResolved)

        round
          .initialize(customDeck_TrumpUnresolved)
          .runA(CoreState.initialize(players, round))
          .value match
          case choosingState: GameState.ChoosingTrump =>
            choosingState.core.hands.getHand(p1.id).value.toList should have size 1
            choosingState.core.trump shouldBe TrumpUnResolved
          case _ => ()
