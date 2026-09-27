package io.github.pallax03.wizard.codecs.engine.model

import io.github.pallax03.wizard.codecs.syntax.CodecSyntax.*
import io.github.pallax03.wizard.engine.model.basic.PlayerId
import io.github.pallax03.wizard.engine.model.events.SystemEvent

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class TestSystemEventCodecs extends AnyWordSpec with Matchers:

  import SystemEventCodecs.given

  "SystemEventCodecs" should:
    "encode SystemEvent correctly" in:
      val p1 = PlayerId(1)
      val event = SystemEvent.joined(p1)
      val jsonString = event.toJson
      jsonString shouldBe """{"type":"system","playerId":1,"action":"joined"}"""
      
    "encode configUpdated correctly" in:
      val p1 = PlayerId(1)
      val event = SystemEvent.configUpdated(p1)
      val jsonString = event.toJson
      jsonString shouldBe """{"type":"system","playerId":1,"action":"config_updated"}"""
