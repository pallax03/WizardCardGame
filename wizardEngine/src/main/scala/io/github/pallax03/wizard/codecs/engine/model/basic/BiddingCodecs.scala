package io.github.pallax03.wizard.codecs.engine.model.basic

import io.circe._
import io.github.pallax03.wizard.engine.model.basic._

object BiddingCodecs:
  import bidding._
  import PlayerIdCodecs.given

  given Encoder[Bids] = Encoder.encodeMap[PlayerId, Bid].asInstanceOf[Encoder[Bids]]
  given Decoder[Bids] = Decoder.decodeMap[PlayerId, Bid].asInstanceOf[Decoder[Bids]]

  given Encoder[Tricks] = Encoder.encodeMap[PlayerId, Trick].asInstanceOf[Encoder[Tricks]]
  given Decoder[Tricks] = Decoder.decodeMap[PlayerId, Trick].asInstanceOf[Decoder[Tricks]]
