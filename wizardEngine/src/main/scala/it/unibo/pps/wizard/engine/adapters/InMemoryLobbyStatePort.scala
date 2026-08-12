package it.unibo.pps.wizard.engine.adapters

import it.unibo.pps.wizard.engine.lobby.Lobby
import it.unibo.pps.wizard.engine.ports.LobbyStatePort

import scala.collection.concurrent.TrieMap
import scala.concurrent.Future

class InMemoryLobbyStatePort extends LobbyStatePort:
  private val store = TrieMap[String, Lobby]()

  override def saveLobby(lobby: Lobby): Future[Unit] =
    store.put(lobby.uuid.toString, lobby)
    Future.successful(())

  override def getLobby(lobbyId: String): Future[Option[Lobby]] =
    Future.successful(store.get(lobbyId))
