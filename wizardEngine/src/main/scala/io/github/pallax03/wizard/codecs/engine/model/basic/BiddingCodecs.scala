package io.github.pallax03.wizard.codecs.engine.model.basic

import io.circe.*

import io.github.pallax03.wizard.engine.model.basic.*

import sttp.tapir.Schema

object BiddingCodecs:
  import bidding._
  import PlayerIdCodecs.given

  // --- Circe ---

  given Encoder[Bids] = Encoder.encodeMap[PlayerId, Bid].asInstanceOf[Encoder[Bids]]
  given Decoder[Bids] = Decoder.decodeMap[PlayerId, Bid].asInstanceOf[Decoder[Bids]]

  given Encoder[Tricks] = Encoder.encodeMap[PlayerId, Trick].asInstanceOf[Encoder[Tricks]]
  given Decoder[Tricks] = Decoder.decodeMap[PlayerId, Trick].asInstanceOf[Decoder[Tricks]]

  // --- Tapir Schemas ---

  given Schema[Bid] = Schema.schemaForInt
  given Schema[Bids] = Schema.schemaForMap[PlayerId, Bid].asInstanceOf[Schema[Bids]]
  given Schema[Tricks] = Schema.schemaForMap[PlayerId, Trick].asInstanceOf[Schema[Tricks]]
