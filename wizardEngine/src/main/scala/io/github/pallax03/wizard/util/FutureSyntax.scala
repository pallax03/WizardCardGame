package io.github.pallax03.wizard.util

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Future, Promise}

import io.vertx.ext.web.RoutingContext

/** Extension methods for interoperability between Vert.x async futures and Scala concurrency. */
object FutureSyntax:
  extension [T](vFuture: io.vertx.core.Future[T])
    /** Converts a Vert.x asynchronous [[io.vertx.core.Future]] to a standard Scala [[scala.concurrent.Future]]. */
    def asScala: Future[T] =
      val p = Promise[T]()
      vFuture.onComplete(ar =>
        if ar.succeeded() then p.success(ar.result()) else p.failure(ar.cause())
      )
      p.future

  extension [T](future: scala.concurrent.Future[T])
    /** Executes completion callbacks safely on the Vert.x event loop context of the [[RoutingContext]]. */
    def onVertxComplete(ctx: RoutingContext)(f: scala.util.Try[T] => Unit): Unit =
      future.onComplete(res => ctx.vertx().runOnContext(_ => f(res)))
