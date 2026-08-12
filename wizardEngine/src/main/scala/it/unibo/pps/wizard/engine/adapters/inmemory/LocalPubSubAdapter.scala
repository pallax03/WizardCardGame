package it.unibo.pps.wizard.engine.adapters.inmemory

import io.vertx.core.Vertx
import io.vertx.core.eventbus.EventBus
import it.unibo.pps.wizard.engine.ports.PubSubPort

import scala.concurrent.Future

class LocalPubSubAdapter(vertx: Vertx) extends PubSubPort:
  private val eventBus: EventBus = vertx.eventBus()

  /** @inheritdoc */
  override def publish(channel: String, jsonMessage: String): Future[Unit] =
    eventBus.publish(channel, jsonMessage)
    Future.successful(())

  /** @inheritdoc */
  override def subscribe(channel: String, onMessage: String => Unit): Future[Unit] =
    eventBus.consumer[String](channel, message => onMessage(message.body()))
    Future.successful(())

  /** @inheritdoc */
  override def unsubscribe(channel: String): Future[Unit] =
    // In Vert.x EventBus, removing specific consumers is handled via the MessageConsumer object.
    // For this simple mock/local adapter, we can ignore unsubscribe or implement a registry of consumers if needed.
    Future.successful(())
