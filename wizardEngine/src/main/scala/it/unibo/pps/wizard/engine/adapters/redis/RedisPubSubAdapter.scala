package it.unibo.pps.wizard.engine.adapters.redis

import it.unibo.pps.wizard.engine.ports.RedisPubSubPort

import scala.concurrent.Future

/**
 * @param redisClient The driver/client used to execute commands against the Redis server.
 *                    (see io.vertx.redis.client.RedisAPI)
 */
class RedisPubSubAdapter( /* redisClient: RedisAPI */ ) extends RedisPubSubPort:
  override def publish(channel: String, jsonMessage: String): Future[Unit] = ???

  override def subscribe(channel: String, onMessage: String => Unit): Future[Unit] = ???

  override def unsubscribe(channel: String): Future[Unit] = ???
