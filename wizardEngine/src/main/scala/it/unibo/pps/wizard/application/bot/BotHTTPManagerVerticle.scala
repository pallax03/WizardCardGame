package it.unibo.pps.wizard.application.bot

import io.vertx.core.AbstractVerticle

/** redisPubSub: this verticle will subscribe for new events, when a bot with playerId and lobbyId corresponding, use [[BotStrategy]] */
class BotHTTPManagerVerticle(
    /* redisPubSub: RedisPubSubPort,
                          wizardAIPort: AIPort */
) extends AbstractVerticle:
  override def start(): Unit = super.start()
  override def stop(): Unit = super.stop()
