package it.unibo.pps.wizard.engine.adapters

import it.unibo.pps.wizard.engine.lobby.{Lobby, LobbyPlayer}
import it.unibo.pps.wizard.engine.model.basic.PlayerId
import it.unibo.pps.wizard.engine.ports.LobbyStatePort

import scala.concurrent.Future

/**
 * Adapter that implements LobbyStatePort to persist Lobby state in a Redis Key-Value store.
 *
 * This is an Infrastructure Driven Adapter. It lives at the boundary of the system
 * and knows how to talk to the actual Redis database.
 *
 * @param redisClient The driver/client used to execute commands against the Redis server.
 *                    (see io.vertx.redis.client.RedisAPI)
 */
class RedisLobbyStateAdapter( /* redisClient: RedisAPI */ ) extends LobbyStatePort:

  /** @inheritdoc */
  override def saveLobby(lobby: Lobby): Future[Unit] =
    // 1. Serialize `lobby` to a JSON string using `LobbyCodecs.given`
    // 2. Execute a Redis SET command: SET "lobby:{lobby.uuid}" "{lobby_json}"
    // 3. Return a successful Future if the Redis command succeeds
    ???

  /** @inheritdoc */
  override def getLobby(lobbyId: String): Future[Option[Lobby]] =
    // 1. Execute a Redis GET command: GET "lobby:{lobbyId}"
    // 2. If it returns null/empty, return Future.successful(None)
    // 3. If it returns a string, deserialize it using `LobbyCodecs.given`
    // 4. Return Future.successful(Some(parsedLobby))
    ???

  /**
   * Atomically adds a player to the lobby, returning the assigned PlayerId if successful.
   * Fails (returns None) if the lobby is full (max 6 players).
   *
   * @param lobbyId the UUID of the lobby.
   * @param player  the player to add (without the final ID, which is assigned and returned).
   * @return a Future containing the assigned PlayerId, or None if the lobby is full.
   */
  override def addPlayer(lobbyId: String, player: LobbyPlayer): Future[Option[PlayerId]] = ???

  /**
   * Atomically removes a player from the lobby by ID.
   *
   * @param lobbyId  the UUID of the lobby.
   * @param playerId the ID of the player to remove.
   * @return a Future containing true if the player was removed, false otherwise.
   */
  override def removePlayer(lobbyId: String, playerId: PlayerId): Future[Boolean] = ???
