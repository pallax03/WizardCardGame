package io.github.pallax03.wizard.codecs.engine.lobby

import io.github.pallax03.wizard.codecs.syntax.CodecSyntax.*
import io.github.pallax03.wizard.engine.lobby.{BotTask, LobbyId}
import io.github.pallax03.wizard.engine.model.basic.PlayerId
import io.github.pallax03.wizard.engine.model.events.InvitationEvent

import org.scalatest.EitherValues.*
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class TestBotTaskCodecs extends AnyWordSpec with Matchers:

  import BotTaskCodecs.given

  "BotTaskCodecs" should:
    "encode and decode BotTask correctly" in:
      val lobbyId = LobbyId("uuid-1234")
      val p1 = PlayerId(1)
      val invitation = InvitationEvent.WaitingForTrump(p1)
      val task = BotTask(lobbyId, invitation)
      val jsonString = task.toJson
      jsonString shouldBe """{"lobbyId":"uuid-1234","invitation":{"event":{"type":"InvitationEvent","action":"WaitingForTrump","destinationId":1,"fields":{"colorOptions":["Blue","Green","Red","Yellow"]}}}}"""
      jsonString.decodeAs[BotTask].value shouldBe task
