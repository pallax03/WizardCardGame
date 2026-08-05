package it.unibo.pps.wizard.codecs.engine.model.basic

import io.circe.*
import it.unibo.pps.wizard.engine.model.basic._

object BiddingCodecs:
  import bidding._
  import PlayerCodecs.given
  
  given Encoder[Bids] = Encoder.encodeMap[PlayerId, Bid].asInstanceOf[Encoder[Bids]]
  given Decoder[Bids] = Decoder.decodeMap[PlayerId, Bid].asInstanceOf[Decoder[Bids]]

  given Encoder[Tricks] = Encoder.encodeMap[PlayerId, Trick].asInstanceOf[Encoder[Tricks]]
  given Decoder[Tricks] = Decoder.decodeMap[PlayerId, Trick].asInstanceOf[Decoder[Tricks]]