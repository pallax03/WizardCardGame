package it.unibo.pps.wizard.application.web.http.responses

import io.circe.Encoder
import io.vertx.ext.web.RoutingContext
import it.unibo.pps.wizard.codecs.syntax.CodecSyntax.*

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

object ResponseHandlers:
  def completeWith[T: Encoder](ctx: RoutingContext)(result: Future[T])(using ExecutionContext): Unit =
    result.onComplete:
      case Success(value) => ctx.response().putHeader("Content-Type", "application/json").end(value.toJsonString)
      case Failure(exception) => ctx.fail(500, exception)

