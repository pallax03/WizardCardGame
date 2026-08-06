package it.unibo.pps.wizard.engine.adapters

import it.unibo.pps.wizard.engine.model.lobby.Lobby
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
