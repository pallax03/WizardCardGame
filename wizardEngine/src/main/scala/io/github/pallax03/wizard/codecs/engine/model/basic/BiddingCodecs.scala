package io.github.pallax03.wizard.codecs.engine.model.basic

import io.circe.*
import sttp.tapir.Schema

import io.github.pallax03.wizard.engine.model.basic.*

object BiddingCodecs:
  import bidding._
  import PlayerIdCodecs.given

  // --- Circe ---

  given Encoder[Bids] = Encoder.encodeMap[PlayerId, Bid].asInstanceOf[Encoder[Bids]]
  given Decoder[Bids] = Decoder.decodeMap[PlayerId, Bid].asInstanceOf[Decoder[Bids]]

  given Encoder[Tricks] = Encoder.encodeMap[PlayerId, Trick].asInstanceOf[Encoder[Tricks]]
  given Decoder[Tricks] = Decoder.decodeMap[PlayerId, Trick].asInstanceOf[Decoder[Tricks]]

  // --- Tapir Schemas ---
  // Bid è type alias Int, Tapir lo risolve automaticamente con schemaForInt.
  // Bids/Tricks sono opaque Map — rappresentati in OpenAPI come oggetto chiave-stringa.

  given Schema[Bid]    = Schema.schemaForInt
  given Schema[Bids]   = Schema.schemaForMap[PlayerId, Bid].asInstanceOf[Schema[Bids]]
  given Schema[Tricks] = Schema.schemaForMap[PlayerId, Trick].asInstanceOf[Schema[Tricks]]
