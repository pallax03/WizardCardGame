package it.unibo.pps.wizard.engine.adapters

import io.vertx.core.Vertx
import it.unibo.pps.wizard.engine.events.ActionEvent
import it.unibo.pps.wizard.engine.events.Event.addressOf
import it.unibo.pps.wizard.engine.events.FailureEvent
import it.unibo.pps.wizard.engine.events.InvitationEvent
import it.unibo.pps.wizard.engine.events.LifecycleEvent
import it.unibo.pps.wizard.engine.events.ProgressEvent
import it.unibo.pps.wizard.engine.events.WizardEvent
import it.unibo.pps.wizard.engine.ports.GameEngineOutboundPort

import scala.concurrent.Future

/**
 * An implementation of the [[GameEngineOutboundPort]] that uses Vert.x event bus to publish events.
 *
 * @param vertx the Vert.x instance used to publish events
 */
class VertxEventBusAdapter(private val vertx: Vertx) extends GameEngineOutboundPort:

  override def publishEvent(event: WizardEvent): Future[Unit] =
    eventAddresses(event).foreach: address =>
      this.vertx.eventBus().publish(address, event)
    Future.successful(())

  override def publishAllEvents(events: List[WizardEvent]): Future[Unit] =
    events.foreach(publishEvent)
    Future.successful(())

  /**
   * Returns a list of addresses to which the given event should be published.
   *
   * @param event the event to be published
   * @return a list of addresses to which the event should be published
   */
  private def eventAddresses(event: WizardEvent): List[String] =
    val familyAddress = event match
      case _: ActionEvent     => addressOf[ActionEvent]
      case _: FailureEvent    => addressOf[FailureEvent]
      case _: InvitationEvent => addressOf[InvitationEvent]
      case _: LifecycleEvent  => addressOf[LifecycleEvent]
      case _: ProgressEvent   => addressOf[ProgressEvent]
    List(event.getClass.getSimpleName, familyAddress, addressOf[WizardEvent]).distinct
