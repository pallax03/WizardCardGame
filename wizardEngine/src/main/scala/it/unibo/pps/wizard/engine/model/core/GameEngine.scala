package it.unibo.pps.wizard.engine.model.core

import it.unibo.pps.wizard.engine.model.basic._
import it.unibo.pps.wizard.engine.model.basic.bidding.Bid
import it.unibo.pps.wizard.engine.model.basic.bidding.Bids
import it.unibo.pps.wizard.engine.model.basic.bidding.Tricks
import it.unibo.pps.wizard.engine.model.basic.cards._
import it.unibo.pps.wizard.engine.model.basic.gameplay._
import it.unibo.pps.wizard.engine.model.events._
import it.unibo.pps.wizard.engine.model.rules._

import GameError._
//import it.unibo.pps.wizard.engine.model.basic.gameplay.Round.next

//import it.unibo.pps.wizard.engine.model.core.InconsistentStateReasons.*

/**
 * The GameEngine is responsible for processing game actions and managing the game state.
 * It takes a GameState and a GameAction as input and produces either a new GameEngine or a GameError.
 */
opaque type GameEngine = (GameState, List[WizardEvent])

/**
 * Companion object for the GameEngine opaque type.
 * Provides methods to process game actions and manage the game state.
 */
object GameEngine:

  extension (engine: GameEngine)
    def state: GameState = engine._1
    def events: List[WizardEvent] = engine._2

  /**
   * Initializes the game engine with the given players.
   * Deals cards, sets the initial game state, and generates relevant events.
   *
   * @param playersIds The playersIds participating in the game.
   * @return A new GameEngine with the initial game state and events.
   */
  def initializeGame(playersIds: List[PlayerId]): GameEngine =
    val initialRound = Round.start
    val initialCore = CoreState.initialize(playersIds, initialRound)

    setupRoundEngine(initialRound, initialCore)

  /**
   * Processes a game action based on the current game state.
   * Returns either a new GameEngine or a GameError if the action is invalid.
   *
   * @param state The current game state.
   * @param action The game action to be processed.
   * @return Either a new GameEngine or a GameError.
   */
  def processAction(state: GameState, action: GameAction): Either[GameError, GameEngine] =
    (state, action) match
      case (currentState: GameState.ChoosingTrump, GameAction.ResolveTrumpColor(playerId, color)) =>
        handleResolveTrump(currentState, playerId, color)

      case (currentState: GameState.Bidding, GameAction.PlaceBid(playerId, bid)) =>
        handlePlaceBid(currentState, playerId, bid)

      case (currentState: GameState.Playing, GameAction.PlayCard(playerId, card)) =>
        handlePlayCard(currentState, playerId, card)

      case (_, _) => Left(InvalidAction)

  /**
   * Handles the action of playing a card during the Playing phase.
   * Validates the player's turn and the card being played, updates the game state accordingly,
   * and generates relevant events.
   *
   * @param currentState The current game state in the Playing phase.
   * @param playerId The ID of the player attempting to play a card.
   * @param card The card being played by the player.
   * @return Either a new GameEngine or a GameError if the action is invalid.
   */
  private def handlePlayCard(
      currentState: GameState.Playing,
      playerId: PlayerId,
      card: Card
  ): Either[GameError, GameEngine] =
    val playerHand = currentState.core.hands.getHand(playerId).getOrElse(Hand.empty)

    for
      _ <- currentState.playerTurn.validateTurnOf(playerId)
      _ <- card.validateAgainst(currentState.table, playerHand)
      updatedHands <- currentState.core.hands
        .remove(playerId, card)
        .toRight(GameError.InconsistentState(InconsistentStateReasons.HandNotFoundFor(playerId)))

      updatedCore = currentState.core.copy(hands = updatedHands)
      updatedTable = currentState.table + (playerId, card)
      winningCard = updatedTable.evaluateTrick(currentState.core.trump)
      followingColor = updatedTable.followingColor
      cardPlayedEvent = ActionEvent.CardPlayed(
        playerId,
        card,
        winningCard,
        followingColor
      )

      finalEngine <-
        if updatedTable.isTrickComplete(updatedCore.playersIds.size) then
          advanceCompletedTrick(currentState, updatedCore, updatedTable, cardPlayedEvent)
        else advanceRegularTurn(currentState, updatedCore, updatedTable, playerId, cardPlayedEvent)
    yield finalEngine

  private def advanceCompletedTrick(
      currentState: GameState.Playing,
      updatedCore: CoreState,
      completedTable: Table,
      cardPlayedEvent: ActionEvent.CardPlayed
  ): Either[GameError, GameEngine] =
    completeTrick(currentState, updatedCore, completedTable).map: engine =>
      (engine.state, cardPlayedEvent +: engine.events)

  private def advanceRegularTurn(
      currentState: GameState.Playing,
      updatedCore: CoreState,
      updatedTable: Table,
      currentPlayerId: PlayerId,
      cardPlayedEvent: ActionEvent.CardPlayed
  ): Either[GameError, GameEngine] =
    val nextPlayer =
      currentState.core.playersIds
        .nextAfter(currentPlayerId)
        .getOrElse(currentState.playerTurn)

    updatedCore.hands
      .getHand(nextPlayer)
      .toRight(GameError.InconsistentState(InconsistentStateReasons.HandNotFoundFor(nextPlayer)))
      .map: nextHand =>
        val nextState = currentState.copy(
          core = updatedCore,
          table = updatedTable,
          playerTurn = nextPlayer
        )
        val invitationEvent =
          InvitationEvent.WaitingForCard(nextPlayer, nextHand.legalCards(updatedTable))
        (nextState, List(cardPlayedEvent, invitationEvent))

  /**
   * Handles the action of placing a bid during the Bidding phase.
   * Validates the player's turn and the bid being placed, updates the game state accordingly,
   * and generates relevant events.
   *
   * @param currentState The current game state in the Bidding phase.
   * @param playerId The ID of the player attempting to place a bid.
   * @param bid The bid being placed by the player.
   * @return Either a new GameEngine or a GameError if the action is invalid.
   */
  private def handlePlaceBid(
      currentState: GameState.Bidding,
      playerId: PlayerId,
      bid: Bid
  ): Either[GameError, GameEngine] =
    val totalPlayers = currentState.core.playersIds.size

    for
      _ <- currentState.playerTurn.validateTurnOf(playerId)
      updatedBids <- BiddingRules.processBid(
        bid,
        currentState.bids,
        playerId,
        currentState.core.round,
        currentState.core.playersIds.size
      )
      bidPlacedEvent = ActionEvent.BidPlaced(playerId, bid)

      finalEngine <-
        if updatedBids.isComplete(totalPlayers) then
          advanceToPlayingPhase(currentState, updatedBids, bidPlacedEvent)
        else advanceToNextBidder(currentState, updatedBids, playerId, bidPlacedEvent)
    yield finalEngine

  private def advanceToPlayingPhase(
      currentState: GameState.Bidding,
      completedBids: Bids,
      bidPlacedEvent: ActionEvent.BidPlaced
  ): Either[GameError, GameEngine] =
    val firstPlayer = currentState.core.round.firstPlayer(currentState.core.playersIds)
    for
      hand <- currentState.core.hands
        .getHand(firstPlayer)
        .toRight(GameError.InconsistentState(InconsistentStateReasons.HandNotFoundFor(firstPlayer)))
      _ <- Either.cond(
        !hand.isEmpty,
        (),
        GameError.InconsistentState(InconsistentStateReasons.HandNotFoundFor(firstPlayer))
      )
      nextState = GameState.Playing(
        core = currentState.core,
        bids = completedBids,
        table = Table.empty,
        playerTurn = firstPlayer,
        tricksWon = Tricks.empty
      )
      events = List(
        ProgressEvent.PhaseChanged(GameState.Playing.toString),
        bidPlacedEvent,
        InvitationEvent.WaitingForCard(firstPlayer, hand.legalCards(Table.empty))
      )
    yield (nextState, events)

  private def advanceToNextBidder(
      currentState: GameState.Bidding,
      updatedBids: Bids,
      currentPlayerId: PlayerId,
      bidPlacedEvent: ActionEvent.BidPlaced
  ): Either[GameError, GameEngine] =
    val nextPlayer =
      currentState.core.playersIds.nextAfter(currentPlayerId).getOrElse(currentState.playerTurn)

    val nextState = currentState.copy(
      bids = updatedBids,
      playerTurn = nextPlayer
    )
    val events = List(
      bidPlacedEvent,
      InvitationEvent.WaitingForBid(nextPlayer, currentState.core.round)
    )
    Right((nextState, events))

  /**
   * Handles the action of resolving the trump color during the ChoosingTrump phase.
   * Validates the player's turn and the color being resolved, updates the game state accordingly,
   * and generates relevant events.
   *
   * @param currentState The current game state in the ChoosingTrump phase.
   * @param playerId The ID of the player attempting to resolve the trump color.
   * @param color The color being resolved as the trump.
   * @return Either a new GameEngine or a GameError if the action is invalid.
   */
  private def handleResolveTrump(
      currentState: GameState.ChoosingTrump,
      playerId: PlayerId,
      color: Card.Color
  ): Either[GameError, GameEngine] =
    for
      _ <- currentState.core.dealerId.validateTurnOf(playerId)
      updatedTrump <- currentState.core.trump resolveWizard color
      trumpResolvedEvent = ActionEvent.TrumpColorResolved(playerId, color)
      finalEngine = advanceToBiddingPhase(currentState, updatedTrump, trumpResolvedEvent)
    yield finalEngine

  private def advanceToBiddingPhase(
      currentState: GameState.ChoosingTrump,
      updatedTrump: Trump,
      trumpResolvedEvent: ActionEvent.TrumpColorResolved
  ): GameEngine =
    val nextState = GameState.Bidding(
      currentState.core.updateTrump(updatedTrump),
      Bids.empty,
      currentState.core.dealerId
    )
    val events = List(
      trumpResolvedEvent,
      ProgressEvent.PhaseChanged(GameState.Bidding.toString),
      InvitationEvent.WaitingForBid(nextState.playerTurn, nextState.core.round)
    )
    (nextState, events)

  private def completeTrick(
      state: GameState.Playing,
      updatedCore: CoreState,
      completedTable: Table
  ): Either[GameError, GameEngine] =
    for
      winningCard <- completedTable
        .evaluateTrick(updatedCore.trump)
        .toRight(GameError.InconsistentState(InconsistentStateReasons.TableNoWinner))

      winnerId <- completedTable
        .playerOf(winningCard)
        .toRight(GameError.InconsistentState(InconsistentStateReasons.TableNoWinner))

      updatedTricks = state.tricksWon addTrickTo winnerId

      trickWonEvent = ProgressEvent.TrickWon(
        winnerId,
        updatedTricks(winnerId),
        completedTable.playedCards
      )

      finalEngine <-
        if isRoundComplete(updatedCore.hands) then
          advanceToRoundCompletion(state, updatedCore, updatedTricks, trickWonEvent)
        else advanceToNextTrickTurn(state, updatedCore, updatedTricks, winnerId, trickWonEvent)
    yield finalEngine

  private def isRoundComplete(hands: Hands): Boolean = hands.areEmpty

  private def advanceToRoundCompletion(
      state: GameState.Playing,
      updatedCore: CoreState,
      updatedTricks: Tricks,
      trickWonEvent: ProgressEvent.TrickWon
  ): Either[GameError, GameEngine] =
    val completedRound = completeRound(state, updatedCore, updatedTricks)
    Right((completedRound.state, trickWonEvent +: completedRound.events))

  private def advanceToNextTrickTurn(
      state: GameState.Playing,
      updatedCore: CoreState,
      updatedTricks: Tricks,
      winnerId: PlayerId,
      trickWonEvent: ProgressEvent.TrickWon
  ): Either[GameError, GameEngine] =
    updatedCore.hands
      .getHand(winnerId)
      .toRight(GameError.InconsistentState(InconsistentStateReasons.HandNotFoundFor(winnerId)))
      .map: hand =>
        val nextState = state.copy(
          core = updatedCore,
          table = Table.empty,
          playerTurn = winnerId,
          tricksWon = updatedTricks
        )
        val legalCards = hand.toList.filter(_.validateAgainst(Table.empty, hand).isRight)
        val invitationEvent = InvitationEvent.WaitingForCard(winnerId, legalCards)

        (nextState, List(trickWonEvent, invitationEvent))

  private def completeRound(
      state: GameState.Playing,
      updatedCore: CoreState,
      updatedTricks: Tricks
  ): GameEngine =
    val updatedScoreboard = ScoringRules.compute(
      updatedCore.playersIds,
      state.bids,
      updatedTricks,
      updatedCore.round,
      updatedCore.scoreboard
    )
    val next = nextRoundOrEnd(updatedCore.copy(scoreboard = updatedScoreboard))
    (
      next.state,
      ProgressEvent.RoundScored(updatedCore.playersIds, updatedScoreboard) +: next.events
    )

  private def nextRoundOrEnd(core: CoreState): GameEngine =
    if core.round.isLastRound(core.playersIds) then
      (
        GameState.Ended(core.playersIds, core.scoreboard),
        List(LifecycleEvent.GameEnded(core.playersIds, core.scoreboard))
      )
    else
      val nextRound = core.round.next
      val nextDealer = core.playersIds.nextAfter(core.dealerId).getOrElse(core.dealerId)
      val updatedCore = core.copy(round = nextRound, dealerId = nextDealer)

      setupRoundEngine(nextRound, updatedCore)

  private def setupRoundEngine(round: Round, coreContext: CoreState): GameEngine =
    val (newCore, gameState) = round.initialize(Deck.create).run(coreContext).value

    val phaseSpecificEvents = gameState match
      case _: GameState.ChoosingTrump =>
        List(InvitationEvent.WaitingForTrump(newCore.dealerId))
      case bidding: GameState.Bidding =>
        List(InvitationEvent.WaitingForBid(bidding.playerTurn, round))
      case _ => Nil

    val commonProgressEvents = List(
      ProgressEvent.CardsDealt(newCore.dealerId, newCore.hands, newCore.trump, newCore.round),
      ProgressEvent.PhaseChanged(gameState.getClass.getSimpleName)
    )

    (gameState, commonProgressEvents ++ phaseSpecificEvents)
