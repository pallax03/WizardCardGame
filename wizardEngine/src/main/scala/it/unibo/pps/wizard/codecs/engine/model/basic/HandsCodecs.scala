package it.unibo.pps.wizard.codecs.engine.model.basic

import io.circe._
import it.unibo.pps.wizard.engine.model.basic._

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
