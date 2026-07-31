package it.unibo.pps.wizard.engine.ports

import it.unibo.pps.wizard.engine.events.WizardEvent

import scala.concurrent.Future

/**
 * Outbound port for the Wizard game engine.
 * This trait defines the methods that can be called by the game engine to publish events to external components.
 */
trait WizardOutboundPort:

  /**
   * Publishes a single event.
   *
   * @param event the event to publish
   * @return a Future indicating the completion of the publish process
   */
  def publishEvent(event: WizardEvent): Future[Unit]

  /**
   * Publishes all events in the list.
   *
   * @param events the list of events to publish
   * @return a Future indicating the completion of the publish process
   */
  def publishAllEvents(events: List[WizardEvent]): Future[Unit]
