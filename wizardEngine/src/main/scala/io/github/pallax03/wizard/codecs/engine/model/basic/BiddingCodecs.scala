package io.github.pallax03.wizard.codecs.engine.model.basic

import io.circe.*

import io.github.pallax03.wizard.engine.model.basic.*

import sttp.tapir.Schema

object BiddingCodecs:
  import bidding._
  import PlayerIdCodecs.given

  given Encoder[Bids] = Encoder.encodeMap[PlayerId, Bid].asInstanceOf[Encoder[Bids]]
  given Decoder[Bids] = Decoder.decodeMap[PlayerId, Bid].asInstanceOf[Decoder[Bids]]

  given Encoder[Tricks] = Encoder.encodeMap[PlayerId, Trick].asInstanceOf[Encoder[Tricks]]
  given Decoder[Tricks] = Decoder.decodeMap[PlayerId, Trick].asInstanceOf[Decoder[Tricks]]

  given Schema[Bid] = Schema.schemaForInt
  given Schema[Bids] = Schema.anyObject[Bids].name(Schema.SName("Bids"))
  given Schema[Tricks] = Schema.anyObject[Tricks].name(Schema.SName("Tricks"))
