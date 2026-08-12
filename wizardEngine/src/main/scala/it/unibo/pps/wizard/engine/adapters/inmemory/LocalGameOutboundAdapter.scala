package it.unibo.pps.wizard.engine.adapters.inmemory

import it.unibo.pps.wizard.engine.ports.GameEngineOutboundPort
import it.unibo.pps.wizard.engine.ports.PubSubPort
import it.unibo.pps.wizard.engine.model.events.WizardEvent
import scala.concurrent.{Future, ExecutionContext}
import it.unibo.pps.wizard.engine.lobby.LobbyId

class LocalGameOutboundAdapter(pubSubPort: PubSubPort) extends GameEngineOutboundPort:

  override def publish(lobbyId: LobbyId, events: WizardEvent*): Future[Unit] =
    val channel = s"lobby-$lobbyId"
    import it.unibo.pps.wizard.codecs.engine.model.WizardEventsCodecs.given
    import it.unibo.pps.wizard.codecs.syntax.CodecSyntax.*
    import scala.concurrent.ExecutionContext.Implicits.global
    
    Future.traverse(events)(event => 
      val jsonString = event.toJsonString
      pubSubPort.publish(channel, jsonString)
    ).map(_ => ())