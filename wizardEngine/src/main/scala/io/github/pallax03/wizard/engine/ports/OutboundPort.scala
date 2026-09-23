package io.github.pallax03.wizard.engine.ports

import scala.concurrent.Future

import io.github.pallax03.wizard.engine.lobby.LobbyId
import io.github.pallax03.wizard.engine.model.events.WizardEvent

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
