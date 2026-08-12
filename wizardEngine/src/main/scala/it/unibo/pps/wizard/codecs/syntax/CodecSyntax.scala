package it.unibo.pps.wizard.codecs.syntax

import io.circe._
import io.circe.parser._
import io.circe.syntax._

object CodecSyntax:
  extension [A: Encoder](a: A)
    def toJsonString: String = a.asJson.noSpaces

  extension (jsonStr: String) def decodeAs[A: Decoder]: Either[Error, A] = decode[A](jsonStr)

  extension (json: Json) def decodeAs[A: Decoder]: Either[Error, A] = json.as[A]
