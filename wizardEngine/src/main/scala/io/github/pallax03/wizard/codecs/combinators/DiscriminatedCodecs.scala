package io.github.pallax03.wizard.codecs.combinators

import io.circe._

object DiscriminatedCodecs:

  extension (json: Json)
    def withTag(tagKey: String, tagValue: String): Json =
      json.deepMerge(Json.obj(tagKey -> Json.fromString(tagValue)))

  def decodeByTag[A](tagKey: String)(resolver: PartialFunction[String, Decoder[A]]): Decoder[A] =
    Decoder.instance: cursor =>
      cursor
        .downField(tagKey)
        .as[String]
        .flatMap: tag =>
          resolver.lift(tag) match
            case Some(decoder) => decoder(cursor)
            case None =>
              Left(DecodingFailure(s"Tag not recognized '$tag' for $tagKey", cursor.history))
