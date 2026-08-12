package it.unibo.pps.wizard.engine.adapters.redis

import it.unibo.pps.wizard.engine.ports.PubSubPort

import scala.concurrent.Future

import io.vertx.redis.client.Redis

class RedisPubSubAdapter(val redis: Redis) extends PubSubPort:
  override def publish(channel: String, jsonMessage: String): Future[Unit] = ???

  override def subscribe(channel: String, onMessage: String => Unit): Future[Unit] = ???

  override def unsubscribe(channel: String): Future[Unit] = ???
