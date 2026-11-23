package io.github.pallax03.wizard.codecs.ws

import io.circe.*
import io.circe.syntax.*

import io.github.pallax03.wizard.application.web.ws.{ChatMessage, WsServerMessage}
import io.github.pallax03.wizard.codecs.engine.model.SystemEventCodecs.given
import io.github.pallax03.wizard.codecs.engine.model.WizardEventsCodecs.given
import io.github.pallax03.wizard.codecs.engine.model.basic.PlayerIdCodecs.given
import io.github.pallax03.wizard.engine.model.basic.PlayerId
import io.github.pallax03.wizard.engine.model.events.{SystemEvent, WizardEvent}

import sttp.tapir.Schema
import sttp.tapir.generic.auto.*

/** Circe codecs and Tapir schemas for WebSocket chat and server event messages. */
object WsMessageCodecs:

  given Encoder[ChatMessage] = Encoder.instance: m =>
    val base = List(
      "type" -> Json.fromString(m.`type`),
      "playerId" -> m.playerId.asJson,
      "text" -> Json.fromString(m.text)
    )
    val optFields = List(
      m.destinationId.map(id => "destinationId" -> id.asJson),
      m.timestamp.map(ts => "timestamp" -> Json.fromString(ts))
    ).flatten
    Json.obj((base ++ optFields)*)

  given Decoder[ChatMessage] = Decoder.instance: c =>
    for
      msgType <- c.downField("type").as[Option[String]]
      _ <-
        if msgType.forall(_ == "message") then Right(())
        else Left(DecodingFailure("Expected type 'message'", c.history))
      playerId <- c.downField("playerId").as[PlayerId]
      text <- c.downField("text").as[String]
      destinationId <- c.downField("destinationId").as[Option[PlayerId]]
      timestamp <- c.downField("timestamp").as[Option[String]]
    yield ChatMessage(playerId, text, destinationId, timestamp)

  given Schema[ChatMessage] = Schema.derived

  given Encoder[WsServerMessage] = Encoder.instance:
    case WsServerMessage.Domain(ev)  => ev.asJson
    case WsServerMessage.System(sys) => sys.asJson
    case WsServerMessage.Chat(chat)  => chat.asJson

  given Decoder[WsServerMessage] = Decoder.instance: c =>
    c.as[ChatMessage]
      .map(WsServerMessage.Chat.apply)
      .orElse(c.as[SystemEvent].map(WsServerMessage.System.apply))
      .orElse(c.as[WizardEvent].map(WsServerMessage.Domain.apply))

  given Schema[WsServerMessage] = Schema.derived
