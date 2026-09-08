package io.github.pallax03.wizard.engine.lobby

import java.util.UUID

import io.github.pallax03.wizard.engine.configuration.GameConfiguration
import io.github.pallax03.wizard.engine.model.basic.PlayerId

/** Represents the status of a Lobby. */
enum LobbyStatus:
  case WAITING, IN_GAME, PAUSED, FINISHED

opaque type LobbyId = String

object LobbyId:
  def apply(id: String): LobbyId = id
  def generate: LobbyId = UUID.randomUUID().toString

enum LobbyError:
  case Full, GameInProgress, GamePaused, NotEnoughPlayers, PlayersOffline, PlayerNotFound, LobbyNotFound,
    NotAuthenticated, GameNotFound
  case GameActionRejected(code: String)

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
      if players.size < WizardRules.MinPlayers then Left(LobbyError.NotEnoughPlayers)
      else if !players.filter(_.isHumanPlaying).forall(_.isOnline) then Left(LobbyError.PlayersOffline)
      else Right(())
    case _                  => Left(LobbyError.GameInProgress)

  def addPlayer(
      name: String,
      difficulty: Option[BotsDifficulty],
      secret: Option[String]
  ): Either[LobbyError, (Player, Lobby)] =
    if status != LobbyStatus.WAITING then Left(LobbyError.GameInProgress)
    else if players.size >= WizardRules.MaxPlayers then Left(LobbyError.Full)
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

  def setPlayerOnlineStatus(playerId: PlayerId, isOnline: Boolean): Either[LobbyError, Lobby] =
    players.indexWhere(_.id == playerId) match
      case -1 => Left(LobbyError.PlayerNotFound)
      case idx =>
        val player = players(idx)
        val updatedPlayer = if isOnline && player.isBot && player.isHuman then player.returnHuman else player.copy(isOnline = isOnline)
        Right(copy(players = players.updated(idx, updatedPlayer), version = version + 1))
