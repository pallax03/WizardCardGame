package it.unibo.pps.wizard.engine.adapters.redis

import it.unibo.pps.wizard.engine.ports.PubSubPort

import scala.concurrent.{Future, Promise}
import io.vertx.redis.client.{Redis, Request, Command, Response}

class RedisPubSubAdapter(val redis: Redis) extends PubSubPort:

  /** @inheritdoc */
  override def publish(channel: String, jsonMessage: String): Future[Unit] =
    val p = Promise[Unit]()
    redis.send(Request.cmd(Command.PUBLISH).arg(channel).arg(jsonMessage))
      .onSuccess(_ => p.success(())).onFailure(p.failure)
    p.future

  /** @inheritdoc */
  override def subscribe(channel: String, onMessage: String => Unit): Future[Unit] =
    val p = Promise[Unit]()
    redis.connect().onSuccess { conn =>
      conn.handler((resp: Response) => if resp.size() == 3 && resp.get(1).toString == channel then onMessage(resp.get(2).toString))
      conn.send(Request.cmd(Command.SUBSCRIBE).arg(channel)).onSuccess(_ => p.success(())).onFailure(p.failure)
    }.onFailure(p.failure)
    p.future

  /** @inheritdoc */
  override def unsubscribe(channel: String): Future[Unit] =
    val p = Promise[Unit]()
    redis.send(Request.cmd(Command.UNSUBSCRIBE).arg(channel))
      .onSuccess(_ => p.success(())).onFailure(p.failure)
    p.future
