package io.github.pallax03.wizard.codecs.engine.model.basic

import io.circe.*

import io.github.pallax03.wizard.engine.model.basic.*

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
