package it.unibo.pps.wizard.application.bot

import io.vertx.core.AbstractVerticle
import it.unibo.pps.wizard.application.bot.strategy.BotStrategy
import it.unibo.pps.wizard.engine.events.FailureEvent
import it.unibo.pps.wizard.engine.events.InvitationEvent
import it.unibo.pps.wizard.engine.events.LifecycleEvent
import it.unibo.pps.wizard.engine.events.PlayerScoped
import it.unibo.pps.wizard.engine.model.basic.PlayerId
import it.unibo.pps.wizard.engine.model.core.GameAction
import it.unibo.pps.wizard.engine.ports.AIPort
import it.unibo.pps.wizard.engine.ports.GameEngineInboundPort

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.Promise
import scala.reflect.ClassTag
import scala.util.Failure
import scala.util.Success

/**
 * A Verticle responsible for managing the lifecycle and the reaction loop of game bots.
 *
 * This component orchestrates bot behavior by:
 * 1. Listening to game lifecycle events to register/deregister bots when a game starts or ends.
 * 2. Subscribing to [[InvitationEvent]] and [[FailureEvent]] to trigger bot logic.
 * 3. Introducing artificial delays (via [[delayed]]) to simulate human thinking time,
 *    ensuring the game flow is not instantaneous.
 *
 * It delegates the actual strategy execution to instances of [[BotStrategy]].
 */
class BotManagerVerticle(
    wizardInboundPort: GameEngineInboundPort,
    wizardAIPort: AIPort
) extends AbstractVerticle:

  private var bots: Map[PlayerId, BotStrategy] = Map.empty
  private var subscriptionIds: List[String] = Nil

  override def start(): Unit =
    println("Starting BotManagerVerticle...")
    wizardInboundPort
      .subscribe[LifecycleEvent]:
        case LifecycleEvent.GameStarted(playersIds) =>
          registerBots(playersIds)
        case _: LifecycleEvent.GameEnded => bots = Map.empty
      .foreach(id => subscriptionIds = id :: subscriptionIds)

    subscribeToEvents[InvitationEvent](1000): (strategy, event) =>
      strategy.resolveInvitationEvents(event)

    subscribeToEvents[FailureEvent](500): (strategy, event) =>
      strategy.resolveFailedEvents(event)

  override def stop(): Unit =
    println("Stopping BotManagerVerticle...")
    wizardInboundPort.unsubscribe(subscriptionIds*)
    bots = Map.empty
    subscriptionIds = Nil

  private def registerBots(playersIds: List[PlayerId]): Unit =
    val _ = (playersIds, wizardAIPort)
    // TODO: BotManagerVerticle should receive the list of bot IDs and their difficulty
    bots = Map.empty

  private def delayed[T](delayMs: Long)(action: => Future[T]): Future[T] =
    val promise = Promise[T]()
    vertx.setTimer(delayMs, _ => promise.completeWith(action))
    promise.future

  private def subscribeToEvents[E <: PlayerScoped: ClassTag](
      delayMs: Long
  )(resolver: (BotStrategy, E) => Future[GameAction]): Unit =
    wizardInboundPort
      .subscribe[E]: event =>
        bots
          .get(event.playerId)
          .foreach: strategy =>
            delayed(delayMs)(resolver(strategy, event)).onComplete:
              case Success(action) =>
                wizardInboundPort.submitAction(action)
              case Failure(error) =>
                println(s"Bot ${event.playerId} failed on $event: ${error.getMessage}")
      .foreach(id => subscriptionIds = id :: subscriptionIds)
