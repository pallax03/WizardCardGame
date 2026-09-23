package io.github.pallax03.wizard.codecs.syntax

import io.circe.*
import io.circe.parser.*
import io.circe.syntax.*

object CodecSyntax:
  extension [A: Encoder](a: A) def toJson: String = a.asJson.noSpaces

  extension (jsonStr: String) def decodeAs[A: Decoder]: Either[Error, A] = decode[A](jsonStr)
  extension (json: Json) def decodeAs[A: Decoder]: Either[Error, A] = json.as[A]
