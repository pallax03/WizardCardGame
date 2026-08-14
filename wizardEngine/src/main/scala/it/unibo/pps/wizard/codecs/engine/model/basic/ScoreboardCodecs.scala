package it.unibo.pps.wizard.codecs.engine.model.basic

import io.circe._
import it.unibo.pps.wizard.engine.model.basic._

object ScoreboardCodecs:

  import gameplay.Round
  import bidding.Bid
  import PlayerIdCodecs.given

  private case class RoundEntry(round: Round, score: Score, bid: Bid)

  private given Codec[RoundEntry] = Codec.from(
    Decoder.forProduct3("round", "score", "bid")(RoundEntry.apply),
    Encoder.forProduct3("round", "score", "bid")(e => (e.round, e.score, e.bid))
  )

  private given Codec[Map[Round, (Score, Bid)]] = Codec.from(
    Decoder[List[RoundEntry]].map(_.map(e => e.round -> (e.score, e.bid)).toMap),
    Encoder[List[RoundEntry]].contramap(m =>
      m.toList.sortBy(_._1).map((r, stats) => RoundEntry(r, stats._1, stats._2))
    )
  )

  given Codec[Scoreboard] = Codec.from(
    Decoder[Map[PlayerId, Map[Round, (Score, Bid)]]].map(Scoreboard.apply),
    Encoder[Map[PlayerId, Map[Round, (Score, Bid)]]].contramap(_.toMap)
  )
