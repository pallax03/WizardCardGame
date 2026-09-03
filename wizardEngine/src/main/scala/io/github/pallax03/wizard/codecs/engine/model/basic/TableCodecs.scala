package io.github.pallax03.wizard.codecs.engine.model.basic

import io.circe.*
import io.circe.syntax.*

import io.github.pallax03.wizard.engine.model.basic.PlayerId
import io.github.pallax03.wizard.engine.model.basic.cards.Card
import io.github.pallax03.wizard.engine.model.basic.gameplay.Table

object TableCodecs:
  import CardCodecs.given
  import PlayerIdCodecs.given

  given Encoder[(PlayerId, Card)] = Encoder.forProduct2("playerId", "card")(identity)
  given Decoder[(PlayerId, Card)] =
    Decoder.forProduct2("playerId", "card")((pId: PlayerId, c: Card) => (pId, c))

  given Encoder[Table] = Encoder.instance: t =>
    val fields = List(
      Some("playedCards" -> t.asInstanceOf[List[(PlayerId, Card)]].asJson),
      t.followingColor.map(col => "followingColor" -> col.asJson)
    ).flatten
    Json.obj(fields*)

  given Decoder[Table] = Decoder.instance: cursor =>
    cursor.downField("playedCards").as[List[(PlayerId, Card)]].map(_.asInstanceOf[Table])
