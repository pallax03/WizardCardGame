package io.github.pallax03.wizard.engine.lobby

import java.util.UUID

import io.github.pallax03.wizard.engine.model.basic.PlayerId

/** Lifecycle status of a game lobby session. */
enum LobbyStatus:
  case WAITING, IN_GAME, DISCONNECTING, PAUSED, FINISHED

  /** Returns `true` if an active game state exists on the engine (in progress, disconnecting, or paused). */
  def existGame: Boolean = this match
    case IN_GAME | DISCONNECTING | PAUSED => true
    case WAITING | FINISHED               => false

opaque type LobbyId = String

object LobbyId:
  def apply(id: String): LobbyId = id
  def generate: LobbyId = UUID.randomUUID().toString

import io.github.pallax03.wizard.engine.model.core.{GameActionError, EntityNotFound, GameException}
import io.github.pallax03.wizard.engine.ports.AIError

/** Domain errors returned by lobby lifecycle and player management operations. */
enum LobbyError:
  case Full, GameInProgress, GamePaused, NotEnoughPlayers, PlayersOffline,
    LobbyNotFound, NotAuthenticated
  case NotFound(err: EntityNotFound)
  case GameActionRejected(err: GameActionError)
  case IAHintError(err: AIError)
  case ConfigurationInvalid(err: ConfigurationErrors)
  case InternalServerError(code: String)

/**
 * Aggregate root managing game room lifecycle, player registration, and distributed session recovery.
 *
 * Enforces room capacity boundaries ([[GameConfiguration.MIN_PLAYERS]] to [[GameConfiguration.MAX_PLAYERS]]),
 * manages secret-based authentication for seat re-attachment, and handles automatic failover to bots
 * when human players disconnect.
 *
 * @param uuid unique identifier for this lobby room.
 * @param players participants currently registered in the lobby.
 * @param status current lifecycle phase of the lobby.
 * @param configuration timing tolerances and strike thresholds.
 * @param version monotonically increasing revision counter for optimistic concurrency control.
 * @param createdAt epoch timestamp when the lobby was initialized.
 */
case class Lobby(
    uuid: LobbyId,
    players: List[Player],
    status: LobbyStatus,
    configuration: GameConfiguration,
    version: Int = 1,
    createdAt: Long = System.currentTimeMillis()
):

  def authenticate(secret: String): Either[LobbyError, Player] =
    players.find(_.secret.contains(secret)).toRight(LobbyError.NotAuthenticated)

  /**
   * Validates whether the lobby can start or resume game execution.
   *
   * Requires status to be `WAITING` or `PAUSED`, at least [[GameConfiguration.MIN_PLAYERS]] (3),
   * and all active human participants to be online.
   */
  def validateStartOrResume: Either[LobbyError, Unit] = status match
    case LobbyStatus.WAITING | LobbyStatus.PAUSED =>
      if players.size < GameConfiguration.MIN_PLAYERS then Left(LobbyError.NotEnoughPlayers)
      else if !players.filter(_.isHumanPlaying).forall(_.isOnline) then
        Left(LobbyError.PlayersOffline)
      else Right(())
    case _ => Left(LobbyError.GameInProgress)

  /**
   * Registers a new player or returns the existing player if the secret matches.
   *
   * If `secret` matches an existing player, operation is idempotent and returns the player unchanged.
   * Otherwise, rejects if the lobby is already in progress or at maximum capacity.
   */
  def addPlayer(
      name: String,
      difficulty: Option[BotsDifficulty],
      secret: Option[String]
  ): Either[LobbyError, (Player, Lobby)] =
    secret.flatMap(s => players.find(_.secret.contains(s))) match
      case Some(existing) => Right(existing -> this)
      case None =>
        if status != LobbyStatus.WAITING then Left(LobbyError.GameInProgress)
        else if players.size >= GameConfiguration.MAX_PLAYERS then Left(LobbyError.Full)
        else
          val newId = PlayerId(if players.isEmpty then 0 else players.map(_.id.toInt).max + 1)
          val newPlayer = difficulty match
            case None    => Player.human(newId, name, secret)
            case Some(d) => Player.bot(newId, d)
          Right(newPlayer -> copy(players = players :+ newPlayer, version = version + 1))

  def removePlayer(playerId: PlayerId): Either[LobbyError, Lobby] =
    val newPlayers = players.filterNot(_.id == playerId)
    if newPlayers.size == players.size then
      Left(LobbyError.NotFound(GameException.PlayerNotFound(playerId)))
    else Right(copy(players = newPlayers, version = version + 1))

  private def evaluateStatus(currentPlayers: List[Player]): LobbyStatus =
    if status == LobbyStatus.WAITING || status == LobbyStatus.FINISHED || status == LobbyStatus.PAUSED
    then status
    else
      val humans = currentPlayers.filter(_.isHumanPlaying)
      if humans.isEmpty then LobbyStatus.PAUSED
      else if humans.forall(_.isOnline) then LobbyStatus.IN_GAME
      else LobbyStatus.DISCONNECTING

  private def modifyPlayer(playerId: PlayerId, updateStatus: Boolean = false)(
      f: Player => Player
  ): Either[LobbyError, Lobby] =
    players.indexWhere(_.id == playerId) match
      case -1 => Left(LobbyError.NotFound(GameException.PlayerNotFound(playerId)))
      case idx =>
        val newPlayers = players.updated(idx, f(players(idx)))
        val newStatus = if updateStatus then evaluateStatus(newPlayers) else status
        Right(copy(players = newPlayers, status = newStatus, version = version + 1))

  def handleOnlineStatusChange(playerId: PlayerId, isOnline: Boolean): Either[LobbyError, Lobby] =
    modifyPlayer(playerId, updateStatus = true): player =>
      if isOnline && player.isHuman then player.returnHuman.copy(strikes = 0)
      else if isOnline then player.copy(isOnline = isOnline, strikes = 0)
      else player.copy(isOnline = isOnline)

  def updateStrikes(playerId: PlayerId, diff: Int): Either[LobbyError, (Int, Lobby)] =
    modifyPlayer(playerId) { p =>
      p.copy(strikes = math.max(0, p.strikes + diff))
    }.map(l => l.players.find(_.id == playerId).get.strikes -> l)

  def resetStrikes(playerId: PlayerId): Either[LobbyError, Lobby] =
    modifyPlayer(playerId)(_.copy(strikes = 0))

  /**
   * Replaces all currently disconnected human players with automated bots to unblock the game.
   *
   * @return pair containing the list of replaced [[PlayerId]]s and the updated lobby.
   */
  def replaceOfflinePlayersWithBots(): (List[PlayerId], Lobby) =
    val offlineIds = players.filter(p => p.isHumanPlaying && !p.isOnline).map(_.id)
    if offlineIds.isEmpty then (Nil, this)
    else
      val newPlayers =
        players.map(p => if offlineIds.contains(p.id) then p.replaceWithABot() else p)
      (
        offlineIds,
        copy(players = newPlayers, status = evaluateStatus(newPlayers), version = version + 1)
      )
