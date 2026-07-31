package it.unibo.pps.wizard.util

import io.vertx.core.Vertx
import io.vertx.core.eventbus.Message

import scala.collection.concurrent.Map
import scala.collection.concurrent.TrieMap
import scala.concurrent.Future
import scala.concurrent.Promise
import scala.util.Try

/**
 * A utility class that allows executing tasks in a Vert.x verticle context.
 *
 * @param vertx the Vert.x instance used to execute tasks
 */
class VerticleExecutor(private val vertx: Vertx):
  import it.unibo.pps.wizard.util.Id
  import VerticleExecutor.*

  private val address: String = Id()
  private val pendingTasks: Map[TaskId, PendingTask[?]] = TrieMap.empty

  this.vertx.eventBus().localConsumer(this.address, this.runPendingTask)

  def runLater[T](task: => T): Future[T] =
    val (taskId, pendingTask) = Id() -> PendingTask(task)
    this.pendingTasks.update(taskId, pendingTask)
    this.vertx.eventBus().publish(this.address, taskId)
    pendingTask.future

  private def runPendingTask(message: Message[TaskId]): Unit =
    this.pendingTasks.get(message.body()).foreach(_.execute())
    this.pendingTasks.remove(message.body())

object VerticleExecutor:
  private type TaskId = String

  private class PendingTask[T](task: => T):
    private val promise: Promise[T] = Promise()

    def execute(): Unit = if !this.promise.isCompleted then this.promise.complete(Try(task))

    def future: Future[T] = this.promise.future
