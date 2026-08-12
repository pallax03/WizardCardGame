package it.unibo.pps.wizard.codecs.engine.model.basic

import it.unibo.pps.wizard.codecs.syntax.CodecSyntax.*
import it.unibo.pps.wizard.engine.model.basic._
import org.scalatest.EitherValues._
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class TestTableCodecs extends AnyWordSpec with Matchers:

  import cards.Card.*
  import gameplay.Table
  import BasicTestDSL.plays
  import TableCodecs.given

  "TableCodecs" should:
    "encode and decode empty Table correctly" in:
      val table = Table.empty
      val jsonString = table.toJson

      jsonString shouldBe """{"playedCards":[]}"""
      jsonString.decodeAs[Table].value shouldBe table

    "encode and decode Table with played cards and followingColor correctly" in:
      val p1 = PlayerId(1)
      val table = Table.empty + (p1 plays (Seven of Blue))
      val jsonString = table.toJson

      jsonString shouldBe """{"playedCards":[{"playerId":1,"card":{"type":"Standard","color":"Blue","rank":7}}],"followingColor":"Blue"}"""
      jsonString.decodeAs[Table].value shouldBe table
