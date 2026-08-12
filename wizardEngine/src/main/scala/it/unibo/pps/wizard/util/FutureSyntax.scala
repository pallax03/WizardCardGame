package it.unibo.pps.wizard.util

import io.vertx.ext.web.RoutingContext

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Future, Promise}

object FutureSyntax:
  extension [T](vFuture: io.vertx.core.Future[T])
    def asScala: Future[T] =
      val p = Promise[T]()
      vFuture.onComplete(ar => if ar.succeeded() then p.success(ar.result()) else p.failure(ar.cause()))
      p.future

  extension [T](future: scala.concurrent.Future[T])
    def onVertxComplete(ctx: RoutingContext)(f: scala.util.Try[T] => Unit): Unit =
      future.onComplete(res => ctx.vertx().runOnContext(_ => f(res)))
