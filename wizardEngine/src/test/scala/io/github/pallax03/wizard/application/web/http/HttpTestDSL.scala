package io.github.pallax03.wizard.application.web.http

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

import io.circe.{Decoder, Encoder}

import io.github.pallax03.wizard.application.web.ApiSupport
import io.github.pallax03.wizard.codecs.engine.lobby.LobbyCodecs.given
import io.github.pallax03.wizard.codecs.syntax.CodecSyntax.*
import io.github.pallax03.wizard.engine.lobby.LobbyError

import sttp.client4.*
import sttp.client4.testing.BackendStub
import sttp.model.Uri
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.server.interceptor.CustomiseInterceptors
import sttp.tapir.server.stub4.TapirStubInterpreter

import org.scalatest.EitherValues
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Seconds, Span}

/** Shared Tapir in-memory HTTP test DSL using Scala 3 context functions and Circe codecs. */
trait HttpTestDSL extends ScalaFutures with EitherValues:

  type HttpResponse = Response[Either[String, String]]

  given PatienceConfig = PatienceConfig(timeout = Span(2, Seconds), interval = Span(15, Millis))
  private val jsonContentType = "application/json"

  private def uri(path: String): Uri = Uri.unsafeParse(s"http://test$path")

  def errorJson(error: LobbyError): String = error.toJson

  extension (res: HttpResponse)
    def bodyAs[A: Decoder]: A = res.body.value.decodeAs[A].value
    def errorBody: String = res.body.left.value

  def withEndpoints(endpoints: List[ServerEndpoint[Any, Future]])(
      test: Backend[Future] ?=> Unit
  ): Unit =
    val options = CustomiseInterceptors[Future, Any](_ => ())
      .exceptionHandler(ApiSupport.exceptionHandler)
    given Backend[Future] =
      TapirStubInterpreter(options, BackendStub.asynchronousFuture)
        .whenServerEndpointsRunLogic(endpoints)
        .backend()
    test

  def get(path: String, bearer: String = "")(using Backend[Future]): HttpResponse =
    send(basicRequest.get(uri(path)), bearer)

  def post(path: String, bearer: String = "")(using Backend[Future]): HttpResponse =
    send(basicRequest.post(uri(path)), bearer)

  def postJson[A: Encoder](path: String, body: A, bearer: String = "")(using
      Backend[Future]
  ): HttpResponse =
    sendJson(basicRequest.post(uri(path)), body, bearer)

  def deleteJson[A: Encoder](path: String, body: A, bearer: String = "")(using
      Backend[Future]
  ): HttpResponse =
    sendJson(basicRequest.delete(uri(path)), body, bearer)

  private def sendJson[A: Encoder](
      req: Request[Either[String, String]],
      body: A,
      bearer: String
  )(using Backend[Future]): HttpResponse =
    send(req.body(body.toJson).contentType(jsonContentType), bearer)

  private def send(req: Request[Either[String, String]], bearer: String)(using
      backend: Backend[Future]
  ): HttpResponse =
    (if bearer.nonEmpty then req.auth.bearer(bearer) else req).send(backend).futureValue
