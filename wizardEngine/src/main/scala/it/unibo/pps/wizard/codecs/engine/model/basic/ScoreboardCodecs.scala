package it.unibo.pps.wizard.codecs.engine.model.basic

import io.circe.*

import it.unibo.pps.wizard.engine.model.basic._

object ScoreboardCodecs:
  import PlayerIdCodecs.given
  import RoundCodecs.given
  
  import gameplay.Round
  import bidding.Bid
  
  given Codec[Scoreboard] = Codec.from(
    Decoder[Map[PlayerId, Map[Round, (Score, Bid)]]].map(Scoreboard.apply),
    Encoder[Map[PlayerId, Map[Round, (Score, Bid)]]].contramap(_.toMap)
  )