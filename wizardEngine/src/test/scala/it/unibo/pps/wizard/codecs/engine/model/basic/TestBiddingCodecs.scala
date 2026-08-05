package it.unibo.pps.wizard.codecs.engine.model.basic

import io.circe.parser.*
import io.circe.syntax.*
import it.unibo.pps.wizard.engine.model.basic.*

import org.scalatest.EitherValues.*
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class TestBiddingCodecs extends AnyWordSpec with Matchers:

  import bidding.Bids

  "BiddingCodecs" should:
    import BiddingCodecs.given
    "encode and decode Bids map correctly" in:
      val bids = Bids.empty + (PlayerId(1) place 2) + (PlayerId(2) place 0)
      val jsonString = bids.asJson.noSpaces
      
      jsonString shouldBe """{"1":2,"2":0}"""
      decode[Bids](jsonString).value shouldBe bids
