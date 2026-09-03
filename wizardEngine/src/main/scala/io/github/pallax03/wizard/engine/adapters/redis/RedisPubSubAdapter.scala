package io.github.pallax03.wizard.engine.adapters.redis

import scala.collection.mutable
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

import cats.syntax.all.*

import io.vertx.redis.client.{Command, Redis, RedisConnection, Request}

import io.github.pallax03.wizard.engine.ports.{PubSubPort, Subscription}
import io.github.pallax03.wizard.util.FutureSyntax.*

class RedisPubSubAdapter(redis: Redis) extends PubSubPort:
  private val handlers = mutable.Map.empty[String, mutable.Set[String => Unit]]

  private lazy val connection: Future[RedisConnection] =
    redis
      .connect()
      .asScala
      .map: conn =>
        conn.handler: resp =>
          if resp.size() == 3 && resp.get(0).toString == "message" then
            handlers
              .getOrElse(resp.get(1).toString, mutable.Set.empty)
              .foreach(_(resp.get(2).toString))
        conn

  /** @inheritdoc */
  override def publish(channel: String, jsonMessage: String): Future[Unit] =
    redis.send(Request.cmd(Command.PUBLISH).arg(channel).arg(jsonMessage)).asScala.void

  /** @inheritdoc */
  override def subscribe(channel: String, onMessage: String => Unit): Future[Subscription] =
    synchronized(handlers.getOrElseUpdate(channel, mutable.Set.empty).add(onMessage))
    connection.flatMap: c =>
      c.send(Request.cmd(Command.SUBSCRIBE).arg(channel))
        .asScala
        .map: _ =>
          new Subscription:
            override def cancel(): Future[Unit] =
              val shouldUnsubscribe = synchronized:
                val callbacks = handlers.getOrElse(channel, mutable.Set.empty)
                callbacks.remove(onMessage)
                val empty = callbacks.isEmpty
                if empty then handlers.remove(channel)
                empty
              if shouldUnsubscribe then
                connection.flatMap(conn =>
                  conn.send(Request.cmd(Command.UNSUBSCRIBE).arg(channel)).asScala.void
                )
              else Future.unit
