package io.github.pallax03.wizard.codecs.ws

import io.github.pallax03.wizard.application.web.ws.{ChatMessage, WsServerMessage}
import io.github.pallax03.wizard.codecs.syntax.CodecSyntax.*
import io.github.pallax03.wizard.engine.model.basic.PlayerId
import io.github.pallax03.wizard.engine.model.events.{LifecycleEvent, SystemEvent}

import org.scalatest.EitherValues.*
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class TestWsMessageCodecs extends AnyWordSpec with Matchers:

  import WsMessageCodecs.given

  "WsMessageCodecs" should:
    "encode and decode ChatMessage with optional fields" in:
      val chat = ChatMessage(PlayerId(1), "Hello!", Some(PlayerId(2)), Some("2026-01-01T00:00:00Z"))
      val json = chat.toJson
      json.decodeAs[ChatMessage].value shouldBe chat

    "encode and decode WsServerMessage variants" in:
      val chatMsg: WsServerMessage = WsServerMessage.Chat(ChatMessage(PlayerId(0), "Hi"))
      val sysMsg: WsServerMessage = WsServerMessage.System(SystemEvent.online(PlayerId(0)))
      val domMsg: WsServerMessage =
        WsServerMessage.Domain(LifecycleEvent.GameStarted(List(PlayerId(0), PlayerId(1))))

      chatMsg.toJson.decodeAs[WsServerMessage].value shouldBe chatMsg
      sysMsg.toJson.decodeAs[WsServerMessage].value shouldBe sysMsg
      domMsg.toJson.decodeAs[WsServerMessage].value shouldBe domMsg
