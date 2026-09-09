package io.github.pallax03.wizard.engine.lobby

import java.util.UUID

import io.github.pallax03.wizard.engine.model.basic.PlayerId

/** Represents the status of a Lobby. */
enum LobbyStatus:
  case WAITING, IN_GAME, DISCONNECTING, PAUSED, FINISHED

  def isGame: Boolean = this match
    case IN_GAME | DISCONNECTING | PAUSED => true
    case WAITING | FINISHED               => false

opaque type LobbyId = String

object LobbyId:
  def apply(id: String): LobbyId = id
  def generate: LobbyId = UUID.randomUUID().toString

enum LobbyError:
  case Full, GameInProgress, GamePaused, NotEnoughPlayers, PlayersOffline, PlayerNotFound,
    LobbyNotFound, NotAuthenticated, GameNotFound
  case GameActionRejected(code: String)
  case ConfigurationInvalid(err: ConfigurationErrors)

case class Lobby(
    uuid: LobbyId,
    players: List[Player],
    status: LobbyStatus,
    configuration: GameConfiguration,
    version: Int = 1
):

  def authenticate(secret: String): Either[LobbyError, Player] =
    players.find(_.secret.contains(secret)).toRight(LobbyError.NotAuthenticated)

  def validateStartOrResume: Either[LobbyError, Unit] = status match
    case LobbyStatus.WAITING | LobbyStatus.PAUSED =>
      if players.size < GameConfiguration.MIN_PLAYERS then Left(LobbyError.NotEnoughPlayers)
      else if !players.filter(_.isHumanPlaying).forall(_.isOnline) then
        Left(LobbyError.PlayersOffline)
      else Right(())
    case _ => Left(LobbyError.GameInProgress)

  def addPlayer(
      name: String,
      difficulty: Option[BotsDifficulty],
      secret: Option[String]
  ): Either[LobbyError, (Player, Lobby)] =
    if status != LobbyStatus.WAITING then Left(LobbyError.GameInProgress)
    else if players.size >= GameConfiguration.MAX_PLAYERS then Left(LobbyError.Full)
    else
      val existing = secret.flatMap(s => players.find(_.secret.contains(s)))
      if existing.isDefined then Right(existing.get -> this)
      else
        val newId = PlayerId(if players.isEmpty then 0 else players.map(_.id.toInt).max + 1)
        val newPlayer = difficulty match
          case None    => Player.human(newId, name, secret)
          case Some(d) => Player.bot(newId, d)
        Right(newPlayer -> copy(players = players :+ newPlayer, version = version + 1))

  def removePlayer(playerId: PlayerId): Either[LobbyError, Lobby] =
    val newPlayers = players.filterNot(_.id == playerId)
    if newPlayers.size == players.size then Left(LobbyError.PlayerNotFound)
    else Right(copy(players = newPlayers, version = version + 1))

  private def evaluateStatus(currentPlayers: List[Player]): LobbyStatus =
    if status == LobbyStatus.WAITING || status == LobbyStatus.FINISHED || status == LobbyStatus.PAUSED
    then status
    else
      val humans = currentPlayers.filter(_.isHumanPlaying)
      if humans.isEmpty then LobbyStatus.PAUSED
      else if humans.forall(_.isOnline) then LobbyStatus.IN_GAME
      else LobbyStatus.DISCONNECTING

  def handleOnlineStatusChange(playerId: PlayerId, isOnline: Boolean): Either[LobbyError, Lobby] =
    players.indexWhere(_.id == playerId) match
      case -1 => Left(LobbyError.PlayerNotFound)
      case idx =>
        val player = players(idx)
        val updatedPlayer =
          if isOnline && player.isBot && player.isHuman then player.returnHuman
          else player.copy(isOnline = isOnline)
        val newPlayers = players.updated(idx, updatedPlayer)
        Right(
          copy(players = newPlayers, status = evaluateStatus(newPlayers), version = version + 1)
        )

  /** Replaces all offline human players with bots and updates the lobby status. */
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
