package it.unibo.pps.wizard.engine.adapters.inmemory

import it.unibo.pps.wizard.engine.lobby.Lobby
import it.unibo.pps.wizard.engine.ports.LobbyStatePort

import scala.collection.concurrent.TrieMap
import scala.concurrent.Future

class LocalLobbyStatePort extends LobbyStatePort:
  private val store = TrieMap[String, Lobby]()

  /** @inheritdoc */
  override def saveLobby(lobby: Lobby): Future[Unit] =
    store.put(lobby.uuid.toString, lobby)
    Future.successful(())

  /** @inheritdoc */
  override def getLobby(lobbyId: it.unibo.pps.wizard.engine.lobby.LobbyId): Future[Option[Lobby]] =
    Future.successful(store.get(lobbyId.toString))

  /** @inheritdoc */
  override def addPlayer(lobbyId: it.unibo.pps.wizard.engine.lobby.LobbyId, name: String, bot: Option[it.unibo.pps.wizard.engine.lobby.BotsDifficulty]): Future[Option[it.unibo.pps.wizard.engine.lobby.Player]] =
    store.get(lobbyId.toString) match
      case Some(lobby) if lobby.players.size < 6 =>
        val newId = it.unibo.pps.wizard.engine.model.basic.PlayerId(lobby.players.size)
        val player = bot.fold(it.unibo.pps.wizard.engine.lobby.Player.human(newId, name))(b => it.unibo.pps.wizard.engine.lobby.Player.bot(newId, b))
        store.put(lobbyId.toString, lobby.addPlayer(player))
        Future.successful(Some(player))
      case Some(_) => Future.successful(None)
      case None =>
        val newId = it.unibo.pps.wizard.engine.model.basic.PlayerId(0)
        val player = bot.fold(it.unibo.pps.wizard.engine.lobby.Player.human(newId, name))(b => it.unibo.pps.wizard.engine.lobby.Player.bot(newId, b))
        val newLobby = it.unibo.pps.wizard.engine.lobby.Lobby(lobbyId, List(player), it.unibo.pps.wizard.engine.lobby.LobbyStatus.WAITING)
        store.put(lobbyId.toString, newLobby)
        Future.successful(Some(player))

  /** @inheritdoc */
  override def removePlayer(lobbyId: it.unibo.pps.wizard.engine.lobby.LobbyId, playerId: it.unibo.pps.wizard.engine.model.basic.PlayerId): Future[Boolean] = ???
