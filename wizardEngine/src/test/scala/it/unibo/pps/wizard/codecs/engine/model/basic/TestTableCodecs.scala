package it.unibo.pps.wizard.codecs.engine.model.basic

import io.circe.parser.*
import io.circe.syntax.*
import it.unibo.pps.wizard.engine.model.basic.PlayerId
import it.unibo.pps.wizard.engine.model.basic.cards.Card.*
import it.unibo.pps.wizard.engine.model.basic.gameplay.Table
import org.scalatest.EitherValues.*
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class TestTableCodecs extends AnyWordSpec with Matchers:

  "TableCodecs" should:
    import TableCodecs.given

    "encode and decode empty Table correctly" in:
      val table = Table.empty
      val jsonString = table.asJson.noSpaces

      jsonString shouldBe """{"playedCards":[]}"""
      decode[Table](jsonString).value shouldBe table

    "encode and decode Table with played cards and followingColor correctly" in:
      val p1 = PlayerId(1)
      val table = Table.empty + (p1 -> (Seven of Blue))
      val jsonString = table.asJson.noSpaces

      jsonString shouldBe """{"playedCards":[{"playerId":1,"card":{"type":"Standard","color":"Blue","rank":7}}],"followingColor":"Blue"}"""
      decode[Table](jsonString).value shouldBe table
