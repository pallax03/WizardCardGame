package io.github.pallax03.wizard.codecs.http

import io.circe.{Decoder, Encoder, Json}
import io.github.pallax03.wizard.application.web.ResponseErrors

import sttp.tapir.Schema

object ResponseErrorsCodec:
  given Encoder[ResponseErrors] = Encoder.instance: err =>
    Json.obj(
      "message" -> Json.fromString(err.message),
      "code" -> Json.fromString(err.code)
    )
  given Decoder[ResponseErrors] = Decoder.instance { c =>
    for
      msg <- c.downField("message").as[String]
      code <- c.downField("code").as[String]
    yield ResponseErrors.UnknownResponseError(msg, code)
  }

  // Schema for Tapir
  given Schema[ResponseErrors] = Schema
    .derived[ResponseErrors.UnknownResponseError]
    .map(err => Some(err: ResponseErrors))(err => ResponseErrors.UnknownResponseError(err.message, err.code))
