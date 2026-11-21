package io.github.pallax03.wizard.application.bot
import io.vertx.core.Future as VertxFuture
import io.vertx.redis.client.{Command, Response}

import io.github.pallax03.wizard.engine.adapters.redis.StubRedis
import io.github.pallax03.wizard.engine.ports.*
import io.github.pallax03.wizard.util.ChannelsKeys

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatest.wordspec.AnyWordSpec

class TestBotManagerVerticle extends AnyWordSpec with Matchers with ScalaFutures:

  given PatienceConfig = PatienceConfig(timeout = Span(5, Seconds), interval = Span(15, Millis))

  private def newVerticle(
      handler: (Command, List[String]) => VertxFuture[Response] = (_, _) =>
        VertxFuture.succeededFuture(null)
  ): (BotManagerVerticle, StubRedis) =
    val redis = StubRedis(handler)
    val inbound = StubInboundPort()
    val verticle = BotManagerVerticle(
      StubAIPort(inbound),
      InMemoryLobbyStatePort(),
      inbound,
      redis
    )
    (verticle, redis)

  "BotManagerVerticle" when:
    "ensuring consumer group" should:
      "issue XGROUP CREATE with MKSTREAM and the requested start ID" in:
        val (verticle, redis) = newVerticle()
        verticle.ensureConsumerGroup("$").futureValue

        redis.commands shouldBe List(
          (
            Command.XGROUP,
            List(
              "CREATE",
              ChannelsKeys.BOT_TASKS_STREAM,
              ChannelsKeys.BOT_CONSUMER_GROUP,
              "$",
              "MKSTREAM"
            )
          )
        )

      "treat BUSYGROUP errors as idempotent success" in:
        val (verticle, _) = newVerticle: (cmd, _) =>
          if cmd == Command.XGROUP then
            VertxFuture.failedFuture(
              new RuntimeException("BUSYGROUP Consumer Group name already exists")
            )
          else VertxFuture.succeededFuture(null)

        verticle.ensureConsumerGroup("$").futureValue shouldBe ()

      "propagate non-BUSYGROUP Redis failures" in:
        val (verticle, _) = newVerticle: (cmd, _) =>
          if cmd == Command.XGROUP then
            VertxFuture.failedFuture(new RuntimeException("ERR connection refused"))
          else VertxFuture.succeededFuture(null)

        val err = verticle.ensureConsumerGroup("$").failed.futureValue
        err.getMessage should include("connection refused")

    "polling and reclaiming stream tasks" should:
      "recreate the missing consumer group from ID 0 when poll receives NOGROUP" in:
        val (verticle, redis) = newVerticle: (cmd, _) =>
          if cmd == Command.XREADGROUP then
            VertxFuture.failedFuture(
              new RuntimeException(
                "NOGROUP No such key 'bot:tasks' or consumer group 'bot_workers' in XREADGROUP with GROUP option"
              )
            )
          else VertxFuture.succeededFuture(null)

        verticle.poll().futureValue shouldBe ()

        redis.commands.map(_._1) shouldBe List(Command.XREADGROUP, Command.XGROUP)
        redis.commands.last._2 shouldBe List(
          "CREATE",
          ChannelsKeys.BOT_TASKS_STREAM,
          ChannelsKeys.BOT_CONSUMER_GROUP,
          "0",
          "MKSTREAM"
        )

      "recreate the missing consumer group from ID 0 when reclaim receives NOGROUP" in:
        val (verticle, redis) = newVerticle: (cmd, _) =>
          if cmd == Command.XAUTOCLAIM then
            VertxFuture.failedFuture(new RuntimeException("NOGROUP No such consumer group"))
          else VertxFuture.succeededFuture(null)

        verticle.reclaim().futureValue shouldBe ()

        redis.commands.map(_._1) shouldBe List(Command.XAUTOCLAIM, Command.XGROUP)
        redis.commands.last._2 shouldBe List(
          "CREATE",
          ChannelsKeys.BOT_TASKS_STREAM,
          ChannelsKeys.BOT_CONSUMER_GROUP,
          "0",
          "MKSTREAM"
        )

      "succeed cleanly when concurrent replica already recreated the group (BUSYGROUP)" in:
        val (verticle, redis) = newVerticle: (cmd, _) =>
          cmd match
            case Command.XREADGROUP =>
              VertxFuture.failedFuture(new RuntimeException("NOGROUP stream missing"))
            case Command.XGROUP =>
              VertxFuture.failedFuture(
                new RuntimeException("BUSYGROUP Consumer Group name already exists")
              )
            case _ => VertxFuture.succeededFuture(null)

        verticle.poll().futureValue shouldBe ()
        redis.commands.map(_._1) shouldBe List(Command.XREADGROUP, Command.XGROUP)

      "not attempt group recreation on non-NOGROUP Redis errors" in:
        val (verticle, redis) = newVerticle: (_, _) =>
          VertxFuture.failedFuture(new RuntimeException("ERR timeout"))

        verticle.poll().futureValue shouldBe ()
        verticle.reclaim().futureValue shouldBe ()

        redis.commands.map(_._1) shouldBe List(Command.XREADGROUP, Command.XAUTOCLAIM)

      "swallow non-BUSYGROUP recreation errors cleanly without retrying in a tight loop" in:
        val (verticle, redis) = newVerticle: (cmd, _) =>
          cmd match
            case Command.XREADGROUP =>
              VertxFuture.failedFuture(new RuntimeException("NOGROUP missing"))
            case Command.XGROUP =>
              VertxFuture.failedFuture(new RuntimeException("ERR Redis unavailable"))
            case _ => VertxFuture.succeededFuture(null)

        verticle.poll().futureValue shouldBe ()
        redis.commands.map(_._1) shouldBe List(Command.XREADGROUP, Command.XGROUP)

    "matching Redis group error messages" should:
      "handle null exception messages safely" in:
        val noMessage = new RuntimeException()
        BotManagerVerticle.isNoGroupError(noMessage) shouldBe false
        BotManagerVerticle.isBusyGroupError(noMessage) shouldBe false
