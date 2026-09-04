package io.github.pallax03.wizard.engine.model.core

import io.github.pallax03.wizard.engine.model.basic.*
import io.github.pallax03.wizard.engine.model.basic.bidding.{Bid, Bids, Tricks}
import io.github.pallax03.wizard.engine.model.basic.cards.*
import io.github.pallax03.wizard.engine.model.basic.gameplay.*
import io.github.pallax03.wizard.engine.model.core.state.{
  GameState,
  ServerCoreState,
  ServerGameState
}
import io.github.pallax03.wizard.engine.model.events.*
import io.github.pallax03.wizard.engine.model.rules.*

/**
 * The GameEngine is responsible for processing game actions and managing the game state.
 * It takes a GameState and a GameAction as input and produces either a new GameEngine or a GameError.
 */
opaque type GameEngine = (ServerGameState, List[WizardEvent])

/**
 * Companion object for the GameEngine opaque type.
 * Provides methods to process game actions and manage the game state.
 */
object GameEngine:

  extension (engine: GameEngine)
    def state: ServerGameState = engine._1
    def events: List[WizardEvent] = engine._2

  /**
   * Initializes the game engine with the given players.
   * Deals cards, sets the initial game state, and generates relevant events.
   *
   * @param playersIds The playersIds participating in the game.
   * @return Either a new GameEngine or a GameError.
   * @throws GameException if an unexpected inconsistent state is encountered during setup.
   */
  def initializeGame(playersIds: List[PlayerId]): GameEngine =
    val initialRound = Round.start
    val initialCore = ServerCoreState.initialize(playersIds, initialRound)
    setupNewRound(initialRound, initialCore)

  /**
   * Recovers a round by reinitializing it with the current core state, generating new hands and trump.
   *
   * @param core The current core state of the game.
   * @return A GameEngine representing the restarted round.
   */
  def recoverRound(core: ServerCoreState): GameEngine =
    setupNewRound(core.round, core)

  /**
   * Processes a game action based on the current game state.
   * Returns either a new GameEngine or a GameError if the action is invalid.
   *
   * @param state The current game state.
   * @param action The game action to be processed.
   * @return Either a new GameEngine or a GameError (domain error).
   * @throws GameException if the server state machine encounters a corrupted state
   *                       (e.g., missing hands, no winner on table).
   */
  def processAction(state: ServerGameState, action: GameAction): Either[GameError, GameEngine] =
    state match
      case currentState @ GameState.ChoosingTrump(_) =>
        action match
          case GameAction.ResolveTrumpColor(playerId, color) =>
            handleResolveTrump(currentState, playerId, color)
          case _ => Left(GameError.InvalidAction)

      case currentState @ GameState.Bidding(_, _, _) =>
        action match
          case GameAction.PlaceBid(playerId, bid) => handlePlaceBid(currentState, playerId, bid)
          case _                                  => Left(GameError.InvalidAction)

      case currentState @ GameState.Playing(_, _, _, _, _) =>
        action match
          case GameAction.PlayCard(playerId, card) => handlePlayCard(currentState, playerId, card)
          case _                                   => Left(GameError.InvalidAction)

      case _ => Left(GameError.InvalidAction)

  /** Handles the action of playing a card during the Playing phase. */
  private def handlePlayCard(
      currentState: GameState.Playing[ServerCoreState],
      playerId: PlayerId,
      card: Card
  ): Either[GameError, GameEngine] =
    for
      _ <- currentState.playerTurn.validateTurnOf(playerId)
      playerHand = currentState.core.hands.getHand(playerId)
      _ <- card.validateAgainst(currentState.table, playerHand)
    yield
      val updatedHands = currentState.core.hands.remove(playerId, card)
      val updatedCore = currentState.core.copy(hands = updatedHands)
      val updatedTable = currentState.table + (playerId, card)
      val winningCard = updatedTable.evaluateTrick(currentState.core.trump)
      val followingColor = updatedTable.followingColor
      val cardPlayedEvent = ActionEvent.CardPlayed(playerId, card, winningCard, followingColor)

      if updatedTable.isTrickComplete(updatedCore.playersIds.size) then
        advanceCompletedTrick(currentState, updatedCore, updatedTable, cardPlayedEvent)
      else advanceRegularTurn(currentState, updatedCore, updatedTable, playerId, cardPlayedEvent)

  private def advanceCompletedTrick(
      currentState: GameState.Playing[ServerCoreState],
      updatedCore: ServerCoreState,
      completedTable: Table,
      cardPlayedEvent: ActionEvent.CardPlayed
  ): GameEngine =
    val engine = completeTrick(currentState, updatedCore, completedTable)
    (engine.state, cardPlayedEvent +: engine.events)

  private def advanceRegularTurn(
      currentState: GameState.Playing[ServerCoreState],
      updatedCore: ServerCoreState,
      updatedTable: Table,
      currentPlayerId: PlayerId,
      cardPlayedEvent: ActionEvent.CardPlayed
  ): GameEngine =
    val nextPlayer =
      currentState.core.playersIds
        .nextAfter(currentPlayerId)
        .getOrElse(currentState.playerTurn)

    val nextHand = updatedCore.hands.getHand(nextPlayer)
    val nextState = currentState.copy(
      core = updatedCore,
      table = updatedTable,
      playerTurn = nextPlayer
    )
    val invitationEvent =
      InvitationEvent.WaitingForCard(nextPlayer, nextHand.legalCards(updatedTable))
    (nextState, List(cardPlayedEvent, invitationEvent))

  /** Handles the action of placing a bid during the Bidding phase. */
  private def handlePlaceBid(
      currentState: GameState.Bidding[ServerCoreState],
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
    yield
      val bidPlacedEvent = ActionEvent.BidPlaced(playerId, bid)
      if updatedBids.isComplete(totalPlayers) then
        advanceToPlayingPhase(currentState, updatedBids, bidPlacedEvent)
      else advanceToNextBidder(currentState, updatedBids, playerId, bidPlacedEvent)

  private def advanceToPlayingPhase(
      currentState: GameState.Bidding[ServerCoreState],
      completedBids: Bids,
      bidPlacedEvent: ActionEvent.BidPlaced
  ): GameEngine =
    val firstPlayer = currentState.core.round.firstPlayer(currentState.core.playersIds)
    val hand = currentState.core.hands.getHand(firstPlayer)
    if hand.isEmpty then throw GameException(InconsistentState.CorruptedHand(firstPlayer))

    val nextState = GameState.Playing(
      core = currentState.core,
      bids = completedBids,
      table = Table.empty,
      playerTurn = firstPlayer,
      tricksWon = Tricks.empty
    )
    val events = List(
      ProgressEvent.PhaseChanged(GameState.Playing.toString),
      bidPlacedEvent,
      InvitationEvent.WaitingForCard(firstPlayer, hand.legalCards(Table.empty))
    )
    (nextState, events)

  private def advanceToNextBidder(
      currentState: GameState.Bidding[ServerCoreState],
      updatedBids: Bids,
      currentPlayerId: PlayerId,
      bidPlacedEvent: ActionEvent.BidPlaced
  ): GameEngine =
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
    (nextState, events)

  /** Handles the action of resolving the trump color during the ChoosingTrump phase. */
  private def handleResolveTrump(
      currentState: GameState.ChoosingTrump[ServerCoreState],
      playerId: PlayerId,
      color: Card.Color
  ): Either[GameError, GameEngine] =
    for
      _ <- currentState.core.dealerId.validateTurnOf(playerId)
      updatedTrump <- currentState.core.trump resolveWizard color
    yield
      val trumpResolvedEvent = ActionEvent.TrumpColorResolved(playerId, color)
      advanceToBiddingPhase(currentState, updatedTrump, trumpResolvedEvent)

  private def advanceToBiddingPhase(
      currentState: GameState.ChoosingTrump[ServerCoreState],
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
      state: GameState.Playing[ServerCoreState],
      updatedCore: ServerCoreState,
      completedTable: Table
  ): GameEngine =
    val winningCard = completedTable
      .evaluateTrick(updatedCore.trump)
      .getOrElse(throw GameException(InconsistentState.TableNoWinner))

    val winnerId = completedTable
      .playerOf(winningCard)
      .getOrElse(throw GameException(InconsistentState.TableNoWinner))

    val updatedTricks = state.tricksWon addTrickTo winnerId

    val trickWonEvent = ProgressEvent.TrickWon(
      winnerId,
      updatedTricks(winnerId),
      completedTable.playedCards
    )

    if isRoundComplete(updatedCore.hands) then
      advanceToRoundCompletion(state, updatedCore, updatedTricks, trickWonEvent)
    else advanceToNextTrickTurn(state, updatedCore, updatedTricks, winnerId, trickWonEvent)

  private def isRoundComplete(hands: Hands): Boolean = hands.areEmpty

  private def advanceToRoundCompletion(
      state: GameState.Playing[ServerCoreState],
      updatedCore: ServerCoreState,
      updatedTricks: Tricks,
      trickWonEvent: ProgressEvent.TrickWon
  ): GameEngine =
    val engine = completeRound(state, updatedCore, updatedTricks)
    (engine.state, trickWonEvent +: engine.events)

  private def advanceToNextTrickTurn(
      state: GameState.Playing[ServerCoreState],
      updatedCore: ServerCoreState,
      updatedTricks: Tricks,
      winnerId: PlayerId,
      trickWonEvent: ProgressEvent.TrickWon
  ): GameEngine =
    val hand = updatedCore.hands.getHand(winnerId)
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
      state: GameState.Playing[ServerCoreState],
      updatedCore: ServerCoreState,
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

  private def nextRoundOrEnd(core: ServerCoreState): GameEngine =
    if core.round.isLastRound(core.playersIds) then
      (
        GameState.Ended(core.playersIds, core.scoreboard),
        List(LifecycleEvent.GameEnded(core.playersIds, core.scoreboard))
      )
    else
      val nextRound = core.round.next
      val nextDealer = core.playersIds.nextAfter(core.dealerId).getOrElse(core.dealerId)
      val updatedCore = core.copy(round = nextRound, dealerId = nextDealer)
      setupNewRound(nextRound, updatedCore)

  private def setupNewRound(
      round: Round,
      coreContext: ServerCoreState
  ): GameEngine =
    val (newCore, gameState) = round.initialize(Deck.create).run(coreContext).value

    val phaseSpecificEvents = gameState match
      case GameState.ChoosingTrump(_) =>
        List(InvitationEvent.WaitingForTrump(newCore.dealerId))
      case GameState.Bidding(_, _, playerTurn) =>
        List(InvitationEvent.WaitingForBid(playerTurn, round))
      case _ => Nil

    val cardsDeals = newCore.playersIds.map: pId =>
      val hand = newCore.hands.getHand(pId)
      ProgressEvent.CardsDealt(pId, hand, newCore.trump, newCore.round)

    (
      gameState,
      (cardsDeals :+ ProgressEvent.PhaseChanged(
        gameState.getClass.getSimpleName
      )) ++ phaseSpecificEvents
    )
