package io.github.pallax03.wizard.codecs.http

import io.circe.{Decoder, Encoder, Json}
import sttp.tapir.Schema
import io.github.pallax03.wizard.engine.errors.AppError

object AppErrorCodecs:
  given Encoder[AppError] = Encoder.instance: err =>
    Json.obj(
      "message" -> Json.fromString(err.message),
      "code" -> Json.fromString(err.code)
    )
  given Decoder[AppError] = Decoder.instance { c =>
    for
      msg <- c.downField("message").as[String]
      code <- c.downField("code").as[String]
    yield AppError.UnknownAppError(msg, code)
  }

  // Schema for Tapir
  given Schema[AppError] = Schema.derived[AppError.UnknownAppError].map(
    err => Some(err: AppError)
  )(err => AppError.UnknownAppError(err.message, err.code))
