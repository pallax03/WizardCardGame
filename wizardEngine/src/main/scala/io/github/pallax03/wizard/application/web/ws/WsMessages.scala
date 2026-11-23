package io.github.pallax03.wizard.application.web.ws

import io.github.pallax03.wizard.engine.model.basic.PlayerId
import io.github.pallax03.wizard.engine.model.events.{SystemEvent, WizardEvent}

/** Bidirectional chat message exchanged over the lobby WebSocket channel. */
case class ChatMessage(
    playerId: PlayerId,
    text: String,
    destinationId: Option[PlayerId] = None,
    timestamp: Option[String] = None,
    `type`: String = "message"
)

/** Union of all server-to-client messages streamed on the lobby WebSocket channel. */
enum WsServerMessage:
  case Domain(event: WizardEvent)
  case System(system: SystemEvent)
  case Chat(chat: ChatMessage)
