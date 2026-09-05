package io.github.pallax03.wizard.application.timer

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success, Try}

import io.vertx.core.AbstractVerticle
import io.vertx.redis.client.{Command, Redis, Request}

import io.github.pallax03.wizard.engine.lobby.LobbyId
import io.github.pallax03.wizard.engine.model.basic.PlayerId
import io.github.pallax03.wizard.engine.ports.{InboundPort, PubSubPort}
import io.github.pallax03.wizard.util.ChannelsKeys

class TurnTimerVerticle(
    pubSubPort: PubSubPort,
    redisClient: Redis,
    inboundPort: InboundPort
) extends AbstractVerticle:

  override def start(): Unit =
    redisClient
      .send(Request.cmd(Command.create("CONFIG")).arg("SET").arg("notify-keyspace-events").arg("Ex"))
      .onComplete(_ => ())

    pubSubPort.subscribe(ChannelsKeys.TURN_TIMER_KEYSPACE, handleExpiredKey)

  private def handleExpiredKey(expiredKey: String): Unit =
    expiredKey.split(':') match
      case Array("timer", lobbyId, playerIdStr) =>
        Try(playerIdStr.toInt).toOption.foreach: pid =>
          inboundPort.handleTimeout(LobbyId(lobbyId), PlayerId(pid)).onComplete:
            case Failure(ex) =>
              pubSubPort.publish(ChannelsKeys.LOGS_CHANNEL, s"ERROR:[TurnTimer] Failed for $lobbyId/$pid: ${ex.getMessage}")
            case Success(_) => ()
      case _ => ()
