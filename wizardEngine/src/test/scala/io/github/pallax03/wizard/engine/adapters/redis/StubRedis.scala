package io.github.pallax03.wizard.engine.adapters.redis

import java.nio.charset.StandardCharsets

import scala.collection.mutable
import scala.jdk.CollectionConverters.*

import io.vertx.core.Future as VertxFuture
import io.vertx.redis.client.impl.RequestImpl
import io.vertx.redis.client.{Command, Redis, RedisConnection, Request, Response}

/** In-memory [[Redis]] test double recording issued commands and returning configured responses. */
class StubRedis(
    handler: (Command, List[String]) => VertxFuture[Response] = (_, _) =>
      VertxFuture.succeededFuture(null)
) extends Redis:
  private val _commands = mutable.ListBuffer.empty[(Command, List[String])]

  def commands: List[(Command, List[String])] = _commands.toList

  private def extractArgs(req: Request): List[String] = req match
    case impl: RequestImpl =>
      impl.getArgs.asScala.toList.map(bytes => new String(bytes, StandardCharsets.UTF_8))
    case _ => Nil

  override def connect(): VertxFuture[RedisConnection] =
    VertxFuture.failedFuture(new UnsupportedOperationException("StubRedis.connect"))

  override def close(): VertxFuture[Void] =
    VertxFuture.succeededFuture(null)

  override def send(command: Request): VertxFuture[Response] =
    val cmd = command.command()
    val args = extractArgs(command)
    _commands.addOne((cmd, args))
    handler(cmd, args)

  override def batch(commands: java.util.List[Request]): VertxFuture[java.util.List[Response]] =
    VertxFuture.failedFuture(new UnsupportedOperationException("StubRedis.batch"))
