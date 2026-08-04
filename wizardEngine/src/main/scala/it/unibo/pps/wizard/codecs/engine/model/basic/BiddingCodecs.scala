package it.unibo.pps.wizard.codecs.engine.model.basic

import io.circe.*
import it.unibo.pps.wizard.engine.model.basic.PlayerId
import it.unibo.pps.wizard.engine.model.basic.bidding.{Bids, Bid}

object BiddingCodecs:
  import PlayerCodecs.given
  
  given Encoder[Bids] = Encoder.encodeMap[PlayerId, Bid].asInstanceOf[Encoder[Bids]]
  given Decoder[Bids] = Decoder.decodeMap[PlayerId, Bid].asInstanceOf[Decoder[Bids]]