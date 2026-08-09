package it.unibo.pps.wizard.codecs.engine.model.core

import io.circe._ 

import it.unibo.pps.wizard.engine.model.core.CoreState

import it.unibo.pps.wizard.engine.model.basic._
import it.unibo.pps.wizard.codecs.engine.model.basic._

object CoreStateCodecs:
//  import PlayerIdCodecs.given
//  import HandsCodecs.given
//  import TrumpCodecs.given
//  import RoundCodecs.given
//  import ScoreboardCodecs.given
  import CardCodecs.given
  import cards.{Hand, Card}
  
  given Codec[Hand] = Codec.from(
    Decoder[List[Card]].map(Hand.apply),
    Encoder[List[Card]].contramap(_.toList),
  )

//  given Codec[Hands] = Codec.from(
//    Decoder[Map[PlayerId, Hand]].map(Hands.apply),
//    Encoder[Map[PlayerId, Hand]].contramap(_.getHand())
//  ) 
  
  given Codec[CoreState] = ??? // Codec.AsObject.derived[CoreState]