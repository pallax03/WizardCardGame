package it.unibo.pps.wizard.util

import io.vertx.core.Vertx
import it.unibo.pps.wizard.util.VerticleExecutor
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.concurrent.duration.SECONDS

class TestVerticleExecutor extends AnyWordSpec with Matchers:
  private val verticleExecutor: VerticleExecutor = VerticleExecutor(Vertx.vertx())
  private val numberOfTasks: Long = 1000
  private val maxDuration: Duration = Duration(5, SECONDS)

  "A verticle executor" should:
    "execute all pending tasks in the same event-loop thread" in:
      java.util.stream.Stream
        .iterate(0, _ + 1)
        .limit(numberOfTasks)
        .parallel()
        .map(_ => verticleExecutor.runLater { Thread.currentThread().getName })
        .map(Await.result(_, maxDuration))
        .distinct()
        .count() shouldBe 1
