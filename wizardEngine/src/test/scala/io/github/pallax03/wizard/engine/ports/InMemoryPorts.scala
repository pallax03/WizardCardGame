package io.github.pallax03.wizard.engine.ports

import scala.collection.mutable
import scala.concurrent.Future

import io.github.pallax03.wizard.engine.lobby.*
import io.github.pallax03.wizard.engine.model.basic.PlayerId
import io.github.pallax03.wizard.engine.model.basic.bidding.Bid
import io.github.pallax03.wizard.engine.model.basic.cards.Card
import io.github.pallax03.wizard.engine.model.core.state.{GameState, PlayerGameState}
import io.github.pallax03.wizard.engine.model.core.{GameAction, GameActionError, GameException}
import io.github.pallax03.wizard.engine.model.events.SystemEvent

/** In-memory [[LobbyStatePort]] test double backed by real [[Lobby]] domain transitions. */
class InMemoryLobbyStatePort(initial: Lobby*) extends LobbyStatePort:
  private val lobbies = mutable.Map.from(initial.map(l => l.uuid -> l))
  private val _clearedStrikes = mutable.ListBuffer.empty[(LobbyId, PlayerId)]
  private val _emittedEvents = mutable.ListBuffer.empty[SystemEvent]

  def clearedStrikes: List[(LobbyId, PlayerId)] = _clearedStrikes.toList
  def emittedEvents: List[SystemEvent] = _emittedEvents.toList
  def currentLobby(lobbyId: LobbyId): Option[Lobby] = lobbies.get(lobbyId)

  override def getLobby(lobbyId: LobbyId): Future[Either[LobbyError, Lobby]] =
    Future.successful(lobbies.get(lobbyId).toRight(LobbyError.LobbyNotFound))

  override def getAuthLobby(
      lobbyId: LobbyId,
      secret: String
  ): Future[Either[LobbyError, (Player, Lobby)]] =
    Future.successful(
      lobbies
        .get(lobbyId)
        .toRight(LobbyError.LobbyNotFound)
        .flatMap(lobby => lobby.authenticate(secret).map(_ -> lobby))
    )

  override def updateLobby[A](lobbyId: LobbyId)(
      f: Lobby => Either[LobbyError, (A, Lobby, Option[SystemEvent])]
  ): Future[Either[LobbyError, A]] =
    Future.successful:
      for
        lobby <- lobbies.get(lobbyId).toRight(LobbyError.LobbyNotFound)
        (res, updated, optEvent) <- f(lobby)
      yield
        lobbies.update(lobbyId, updated)
        optEvent.foreach(_emittedEvents.addOne)
        res

  override def deleteLobby(lobbyId: LobbyId): Future[Either[LobbyError, Unit]] =
    Future.successful:
      lobbies.remove(lobbyId)
      Right(())

  override def addPlayer(
      lobbyId: LobbyId,
      name: String,
      difficulty: Option[BotsDifficulty] = None,
      secret: Option[String] = None
  ): Future[Either[LobbyError, Player]] =
    Future.successful:
      val lobby = lobbies.getOrElse(
        lobbyId,
        Lobby(lobbyId, Nil, LobbyStatus.WAITING, GameConfiguration())
      )
      lobby
        .addPlayer(name, difficulty, secret)
        .map: (player, updated) =>
          lobbies.update(lobbyId, updated)
          player

  override def setPlayerOnlineStatus(
      lobbyId: LobbyId,
      playerId: PlayerId,
      isOnline: Boolean
  ): Future[Option[LobbyStatus]] =
    Future.successful:
      lobbies
        .get(lobbyId)
        .flatMap(_.handleOnlineStatusChange(playerId, isOnline).toOption)
        .map: updated =>
          lobbies.update(lobbyId, updated)
          updated.status

  override def clearPlayerStrikes(lobbyId: LobbyId, playerId: PlayerId): Future[Unit] =
    _clearedStrikes.addOne((lobbyId, playerId))
    lobbies
      .get(lobbyId)
      .flatMap(_.resetStrikes(playerId).toOption)
      .foreach: updated =>
        lobbies.update(lobbyId, updated)
    Future.unit

  override def incrementPlayerStrikes(lobbyId: LobbyId, playerId: PlayerId): Future[Int] =
    Future.successful:
      lobbies
        .get(lobbyId)
        .flatMap(_.updateStrikes(playerId, 1).toOption)
        .map: (count, updated) =>
          lobbies.update(lobbyId, updated)
          count
        .getOrElse(0)

/** In-memory [[InboundPort]] test double recording invocations and returning configured responses. */
class StubInboundPort(
    stateHandler: (LobbyId, PlayerId) => Future[PlayerGameState] = (_, _) =>
      Future.failed(GameException.GameNotFound),
    actionResult: Future[Either[GameActionError, Unit]] = Future.successful(Right(()))
) extends InboundPort:
  private val _startedGames = mutable.ListBuffer.empty[(LobbyId, List[PlayerId], GameConfiguration)]
  private val _resumedGames = mutable.ListBuffer.empty[LobbyId]
  private val _deletedGames = mutable.ListBuffer.empty[LobbyId]
  private val _submittedActions = mutable.ListBuffer.empty[(LobbyId, GameAction)]
  private val _forcedFallbacks = mutable.ListBuffer.empty[(LobbyId, PlayerId)]

  def startedGames: List[(LobbyId, List[PlayerId], GameConfiguration)] = _startedGames.toList
  def resumedGames: List[LobbyId] = _resumedGames.toList
  def deletedGames: List[LobbyId] = _deletedGames.toList
  def submittedActions: List[(LobbyId, GameAction)] = _submittedActions.toList
  def forcedFallbacks: List[(LobbyId, PlayerId)] = _forcedFallbacks.toList

  override def getState(lobbyId: LobbyId, playerId: PlayerId): Future[PlayerGameState] =
    stateHandler(lobbyId, playerId)

  override def startGame(
      lobbyId: LobbyId,
      players: List[PlayerId],
      config: GameConfiguration
  ): Future[Unit] =
    _startedGames.addOne((lobbyId, players, config))
    Future.unit

  override def resumeGame(lobbyId: LobbyId): Future[Unit] =
    _resumedGames.addOne(lobbyId)
    Future.unit

  override def deleteGame(lobbyId: LobbyId): Future[Unit] =
    _deletedGames.addOne(lobbyId)
    Future.unit

  override def submitAction(
      lobbyId: LobbyId,
      action: GameAction
  ): Future[Either[GameActionError, Unit]] =
    _submittedActions.addOne((lobbyId, action))
    actionResult

  override def forceFallbackAction(lobbyId: LobbyId, playerId: PlayerId): Future[Unit] =
    _forcedFallbacks.addOne((lobbyId, playerId))
    Future.unit

object StubInboundPort:
  def returning(state: PlayerGameState): StubInboundPort =
    StubInboundPort(stateHandler = (_, _) => Future.successful(state))

  def failing(error: Throwable): StubInboundPort =
    StubInboundPort(stateHandler = (_, _) => Future.failed(error))

/** In-memory [[AIPort]] test double returning fixed hints for each matching game phase. */
class StubAIPort(
    inbound: InboundPort,
    trump: Option[Card.Color] = None,
    bid: Option[Bid] = None,
    card: Option[Card] = None
) extends AIPort(inbound):
  override protected def resolveTrumpColorLogic(
      playerId: PlayerId
  ): PartialFunction[PlayerGameState, Option[Card.Color]] =
    case _: GameState.ChoosingTrump[?] => trump

  override protected def placeBidLogic(
      playerId: PlayerId
  ): PartialFunction[PlayerGameState, Option[Bid]] =
    case _: GameState.Bidding[?] => bid

  override protected def bestCardLogic(
      playerId: PlayerId
  ): PartialFunction[PlayerGameState, Option[Card]] =
    case _: GameState.Playing[?] => card
