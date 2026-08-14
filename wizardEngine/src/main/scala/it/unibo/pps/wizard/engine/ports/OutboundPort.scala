package it.unibo.pps.wizard.engine.ports

import it.unibo.pps.wizard.engine.lobby.LobbyId
import it.unibo.pps.wizard.engine.model.events.WizardEvent

import scala.concurrent.Future

/**
 * Outbound port for the Wizard game engine.
 * This trait defines the methods that can be called by the game engine to publish events to external components.
 */
trait OutboundPort:

  /**
   * Publishes events to the external infrastructure.
   *
   * @param lobbyId the identifier of the lobby
   * @param events the events to publish
   * @return a Future indicating the completion of the publish process
   */
  def publish(lobbyId: LobbyId, events: WizardEvent*): Future[Unit]
