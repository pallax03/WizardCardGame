package io.github.pallax03.wizard.engine.model.core

import io.github.pallax03.wizard.engine.model.basic.*
import io.github.pallax03.wizard.engine.model.basic.bidding.{Bid, Bids, Tricks}
import io.github.pallax03.wizard.engine.model.basic.cards.*
import io.github.pallax03.wizard.engine.model.basic.gameplay.*
import io.github.pallax03.wizard.engine.model.core.state.{GameState, ServerCoreState, ServerGameState}
import io.github.pallax03.wizard.engine.model.events.*
import io.github.pallax03.wizard.engine.model.rules.*

import scala.language.postfixOps

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
  extension (state: ServerGameState)
    private def toGameEngine(event: WizardEvent): GameEngine = (state, List(event))
    private def toGameEngine(events: WizardEvent*): GameEngine = (state, events.toList)
  extension (flow: GameEngine)
    def state: ServerGameState = flow._1
    def events: List[WizardEvent] = flow._2
    private def prepend(event: WizardEvent): GameEngine = (flow.state, event +: flow.events)

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
    setupNewRound(initialRound, initialCore).prepend(LifecycleEvent.GameStarted(playersIds))

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
          case GameAction.ResolveTrumpColor(playerId, color) => handleResolveTrump(currentState, playerId, color)
          case _ => Left(GameError.InvalidAction(state.pendingInvitation(action.playerId)))

      case currentState @ GameState.Bidding(_, _, _) =>
        action match
          case GameAction.PlaceBid(playerId, bid) => handlePlaceBid(currentState, playerId, bid)
          case _ => Left(GameError.InvalidAction(state.pendingInvitation(action.playerId)))

      case currentState @ GameState.Playing(_, _, _, _, _) =>
        action match
          case GameAction.PlayCard(playerId, card) => handlePlayCard(currentState, playerId, card)
          case _ => Left(GameError.InvalidAction(state.pendingInvitation(action.playerId)))

      case _ => Left(GameError.InvalidAction(state.pendingInvitation(action.playerId)))

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

      val flow = if updatedTable.isTrickComplete(updatedCore.playersIds.size) then
        completeTrick(currentState, updatedCore, updatedTable)
      else advanceRegularTurn(currentState, updatedCore, updatedTable, playerId)
      flow.prepend(ActionEvent.CardPlayed(playerId, card, updatedTable.evaluateTrick(currentState.core.trump), updatedTable.followingColor))

  private def advanceRegularTurn(
      currentState: GameState.Playing[ServerCoreState],
      updatedCore: ServerCoreState,
      updatedTable: Table,
      currentPlayerId: PlayerId
  ): GameEngine =
    val nextPlayer =
      currentState.core.playersIds
        .nextAfter(currentPlayerId)

    val nextHand = updatedCore.hands.getHand(nextPlayer)
    currentState.copy(
      core = updatedCore,
      table = updatedTable,
      playerTurn = nextPlayer
    ).toGameEngine(InvitationEvent.WaitingForCard(nextPlayer, nextHand.legalCards(updatedTable)))

  /** Handles the action of placing a bid during the Bidding phase. */
  private def handlePlaceBid(
      currentState: GameState.Bidding[ServerCoreState],
      playerId: PlayerId,
      bid: Bid
  ): Either[GameError, GameEngine] =
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
      val flow = if updatedBids.isComplete(currentState.core.playersIds.size) then
        advanceToPlayingPhase(currentState, updatedBids)
      else advanceToNextBidder(currentState, updatedBids, playerId)
      flow.prepend(ActionEvent.BidPlaced(playerId, bid))

  private def advanceToPlayingPhase(
      currentState: GameState.Bidding[ServerCoreState],
      completedBids: Bids,
  ): GameEngine =
    val firstPlayer = currentState.core.round.firstPlayer(currentState.core.playersIds)
    val hand = currentState.core.hands.getHand(firstPlayer)
    if hand.isEmpty then throw GameException(InconsistentState.CorruptedHand(firstPlayer))

    GameState.Playing(
      core = currentState.core,
      bids = completedBids,
      table = Table.empty,
      playerTurn = firstPlayer,
      tricksWon = Tricks.empty
    ).toGameEngine(
      ProgressEvent.PhaseChanged(GameState.Playing.toString),
      InvitationEvent.WaitingForCard(firstPlayer, hand.legalCards(Table.empty))
    )

  private def advanceToNextBidder(
      currentState: GameState.Bidding[ServerCoreState],
      updatedBids: Bids,
      currentPlayerId: PlayerId,
  ): GameEngine =
    val nextPlayer =
      currentState.core.playersIds.nextAfter(currentPlayerId)
    currentState.copy(
      bids = updatedBids,
      playerTurn = nextPlayer
    ).toGameEngine(InvitationEvent.WaitingForBid(nextPlayer, currentState.core.round, updatedBids.notValidBid(currentState.core.round, currentState.core.playersIds.size)))

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
    GameState.Bidding(
      currentState.core.updateTrump(updatedTrump),
      Bids.empty,
      currentState.core.dealerId
    ).toGameEngine(
      trumpResolvedEvent,
      ProgressEvent.PhaseChanged(GameState.Bidding.toString),
      InvitationEvent.WaitingForBid(currentState.core.dealerId, currentState.core.round, Bids.empty.notValidBid(currentState.core.round, currentState.core.playersIds.size))
    )

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

    val updatedTricks = state.tricksWon addTrickTo winnerId

    val flow = if isRoundComplete(updatedCore.hands) then
      completeRound(state, updatedCore, updatedTricks)
    else advanceToNextTrickTurn(state, updatedCore, updatedTricks, winnerId)
    flow.prepend(ProgressEvent.TrickWon(
      winnerId,
      updatedTricks(winnerId),
      completedTable.playedCards
    ))

  private def isRoundComplete(hands: Hands): Boolean = hands.areEmpty

  private def advanceToNextTrickTurn(
      state: GameState.Playing[ServerCoreState],
      updatedCore: ServerCoreState,
      updatedTricks: Tricks,
      winnerId: PlayerId
  ): GameEngine =
    state.copy(
      core = updatedCore,
      table = Table.empty,
      playerTurn = winnerId,
      tricksWon = updatedTricks
    ).toGameEngine(InvitationEvent.WaitingForCard(winnerId, updatedCore.hands.getHand(winnerId).legalCards(Table.empty)))

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
    nextRoundOrEnd(updatedCore.copy(scoreboard = updatedScoreboard)).prepend(ProgressEvent.RoundScored(updatedCore.playersIds, updatedScoreboard))

  private def nextRoundOrEnd(core: ServerCoreState): GameEngine =
    if core.round.isLastRound(core.playersIds) then
      GameState.Ended(core.playersIds, core.scoreboard).toGameEngine(LifecycleEvent.GameEnded(core.playersIds, core.scoreboard))
    else
      val nextRound = core.round.next
      val nextDealer = core.playersIds.nextAfter(core.dealerId)
      setupNewRound(nextRound, core.copy(round = nextRound, dealerId = nextDealer))

  private def setupNewRound(
      round: Round,
      coreContext: ServerCoreState
  ): GameEngine =
    val (newCore, gameState) = round.initialize(Deck.create).run(coreContext).value

    val invitationEvents: List[WizardEvent] = gameState match
      case GameState.ChoosingTrump(_) =>
        List(InvitationEvent.WaitingForTrump(newCore.dealerId))
      case GameState.Bidding(_, _, playerTurn) =>
        List(InvitationEvent.WaitingForBid(playerTurn, round, Bids.empty.notValidBid(round, newCore.playersIds.size)))
      case _ => List.empty

    val cardsDeals: List[WizardEvent] = newCore.playersIds.map: pId =>
      ProgressEvent.CardsDealt(pId, newCore.hands.getHand(pId), newCore.trump, newCore.round)
      
    val allEvents = cardsDeals ::: ProgressEvent.PhaseChanged(gameState.getClass.getSimpleName) :: invitationEvents
    gameState.toGameEngine(allEvents*)
