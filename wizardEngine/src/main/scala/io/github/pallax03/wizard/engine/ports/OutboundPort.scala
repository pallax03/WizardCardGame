package io.github.pallax03.wizard.engine.ports

import scala.concurrent.Future

import io.github.pallax03.wizard.engine.lobby.LobbyId
import io.github.pallax03.wizard.engine.model.events.WizardEvent

/**
 * Secondary driven port in the hexagonal architecture for external event dissemination.
 *
 * Dispatches domain [[WizardEvent]] sequences emitted by the game engine during state
 * transitions to external infrastructure adapters (Redis Pub/Sub, WebSocket broadcasters).
 */
trait OutboundPort:

  /**
   * Publishes domain events to external listeners.
   *
   * Handles routing for both broadcast events ([[io.github.pallax03.wizard.engine.model.events.WizardEvent.PlayerScoped]])
   * and private unicast events ([[io.github.pallax03.wizard.engine.model.events.WizardEvent.DestinationScoped]]).
   */
  def publish(lobbyId: LobbyId, events: WizardEvent*): Future[Unit]
