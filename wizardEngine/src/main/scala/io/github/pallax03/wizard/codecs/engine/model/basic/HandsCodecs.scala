package io.github.pallax03.wizard.codecs.engine.model.basic

import io.circe.*

import io.github.pallax03.wizard.engine.model.basic.*

import sttp.tapir.Schema

/** Circe codecs and Tapir schemas for individual player hands and the round hands collection. */
object HandsCodecs:
  import CardCodecs.given
  import PlayerIdCodecs.given
  import cards.{Hands, Hand, Card}

  given Codec[Hand] = Codec.from(
    Decoder[List[Card]].map(Hand.apply),
    Encoder[List[Card]].contramap(_.toList)
  )

  given Codec[Hands] = Codec.from(
    Decoder[Map[PlayerId, Hand]].map(Hands.apply),
    Encoder[Map[PlayerId, Hand]].contramap(_.toMap)
  )

  given Schema[Hand] = summon[Schema[List[Card]]].as[Hand].name(Schema.SName("Hand"))
  given Schema[Hands] =
    Schema.schemaForMap[PlayerId, Hand](_.toString).as[Hands].name(Schema.SName("Hands"))
