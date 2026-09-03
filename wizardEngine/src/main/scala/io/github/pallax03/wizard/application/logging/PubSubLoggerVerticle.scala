package io.github.pallax03.wizard.application.logging

import io.vertx.core.AbstractVerticle
import org.slf4j.LoggerFactory
import io.github.pallax03.wizard.engine.ports.PubSubPort
import io.github.pallax03.wizard.util.ChannelsKeys

class PubSubLoggerVerticle(pubSubPort: PubSubPort) extends AbstractVerticle:
  private val logger = LoggerFactory.getLogger("PubSubLogger")

  override def start(): Unit =
    pubSubPort.subscribe(ChannelsKeys.LOGS_CHANNEL, msg => {
      if (msg.startsWith("ERROR:")) logger.error(msg.stripPrefix("ERROR:"))
      else if (msg.startsWith("WARN:")) logger.warn(msg.stripPrefix("WARN:"))
      else logger.info(msg.stripPrefix("INFO:"))
    })
