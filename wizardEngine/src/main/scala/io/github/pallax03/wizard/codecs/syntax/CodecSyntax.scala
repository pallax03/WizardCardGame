package io.github.pallax03.wizard.codecs.syntax

import io.circe._
import io.circe.parser._
import io.circe.syntax._

object CodecSyntax:
  extension [A: Encoder](a: A) def toJson: String = a.asJson.noSpaces

  extension (jsonStr: String) def decodeAs[A: Decoder]: Either[Error, A] = decode[A](jsonStr)
  extension (json: Json) def decodeAs[A: Decoder]: Either[Error, A] = json.as[A]
