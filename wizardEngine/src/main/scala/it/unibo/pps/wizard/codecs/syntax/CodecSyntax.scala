package it.unibo.pps.wizard.codecs.syntax

import io.circe._, io.circe.syntax._, io.circe.parser._

object CodecSyntax:
  extension [A: Encoder](a: A)
    def toJson: Json = a.asJson
    def toJsonString: String = a.asJson.noSpaces
    
  extension (jsonStr: String)
    def decodeAs[A: Decoder]: Either[Error, A] = decode[A](jsonStr)

  extension (json: Json)
    def decodeAs[A: Decoder]: Either[Error, A] = json.as[A]
