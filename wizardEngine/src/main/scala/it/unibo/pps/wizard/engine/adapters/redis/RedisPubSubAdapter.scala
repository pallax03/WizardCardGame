package it.unibo.pps.wizard.engine.adapters.redis

import io.vertx.redis.client.Command
import io.vertx.redis.client.Redis
import io.vertx.redis.client.RedisConnection
import io.vertx.redis.client.Request
import it.unibo.pps.wizard.engine.ports.PubSubPort
import it.unibo.pps.wizard.util.FutureSyntax._

import scala.collection.mutable
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

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

  override def publish(channel: String, jsonMessage: String): Future[Unit] =
    redis.send(Request.cmd(Command.PUBLISH).arg(channel).arg(jsonMessage)).asScala.map(_ => ())

  override def subscribe(channel: String, onMessage: String => Unit): Future[Unit] =
    synchronized(handlers.getOrElseUpdate(channel, mutable.Set.empty).add(onMessage))
    connection.flatMap(c =>
      c.send(Request.cmd(Command.SUBSCRIBE).arg(channel)).asScala.map(_ => ())
    )

  override def unsubscribe(channel: String): Future[Unit] =
    synchronized(handlers.remove(channel))
    connection.flatMap(c =>
      c.send(Request.cmd(Command.UNSUBSCRIBE).arg(channel)).asScala.map(_ => ())
    )
