package it.unibo.pps.wizard.codecs.engine.model.basic

import it.unibo.pps.wizard.codecs.syntax.CodecSyntax._
import it.unibo.pps.wizard.engine.model.basic._
import org.scalatest.EitherValues._
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class TestBiddingCodecs extends AnyWordSpec with Matchers:

  import bidding.{Bids, Tricks}
  import BiddingCodecs.given

  "BiddingCodecs" should:
    val p1: PlayerId = PlayerId(1)
    val p2: PlayerId = PlayerId(2)
    "encode and decode Bids map correctly" in:
      val bids = Bids.empty + (p1 place 2) + (p2 place 0)
      val jsonString = bids.toJson
      jsonString shouldBe """{"1":2,"2":0}"""
      jsonString.decodeAs[Bids].value shouldBe bids

    "encode and decode Tricks map correctly" in:
      val tricks = Tricks.empty addTrickTo p1 addTrickTo p2 addTrickTo p2
      val jsonString = tricks.toJson
      jsonString shouldBe """{"1":1,"2":2}"""
      jsonString.decodeAs[Tricks].value shouldBe tricks
