package io.github.pallax03.wizard.application.bot

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.jdk.CollectionConverters.*
import scala.util.{Failure, Random, Success}

import cats.data.OptionT
import cats.syntax.all.*

import io.vertx.core.AbstractVerticle
import io.vertx.redis.client.{Command, Redis, Request, Response}

import io.github.pallax03.wizard.application.bot.strategy.BotStrategy
import io.github.pallax03.wizard.codecs.engine.lobby.BotTaskCodecs.given
import io.github.pallax03.wizard.codecs.syntax.CodecSyntax.*
import io.github.pallax03.wizard.engine.lobby.{BotTask, LobbyId}
import io.github.pallax03.wizard.engine.model.core.GameAction
import io.github.pallax03.wizard.engine.model.events.InvitationEvent
import io.github.pallax03.wizard.engine.ports.*
import io.github.pallax03.wizard.util.FutureSyntax.*
import io.github.pallax03.wizard.util.{ChannelsKeys, WizardLogger}

/**
 * Vert.x worker verticle executing automated AI bot turns.
 *
 * Distributed Processing Model:
 *  - Joins the Redis consumer group `bot_workers` on the `bot:tasks` stream.
 *  - Competes with peer worker instances to poll incoming [[BotTask]] entries.
 *  - Periodically executes `XAUTOCLAIM` to reclaim abandoned tasks from failed worker nodes.
 *  - Automatically recreates the consumer group from ID `0` if `NOGROUP` is detected at runtime.
 *  - Resolves player difficulty ([[io.github.pallax03.wizard.engine.lobby.BotsDifficulty]]) and invokes [[BotStrategy]].
 *  - Injects a realistic delay (800ms-2000ms, +5s after trick/round recap) to simulate human think time.
 *  - Submits the chosen action to [[InboundPort]] with guaranteed fallback to [[FallbackStrategy]] on rejection.
 *  - Acknowledges stream entries via `XACK` upon completion.
 */
class BotManagerVerticle(
    prologPort: AIPort,
    lobbyStatePort: LobbyStatePort,
    gameInboundPort: InboundPort,
    redisClient: Redis
) extends AbstractVerticle:

  import BotManagerVerticle.*
  private val consumerId = s"bot-${java.util.UUID.randomUUID()}"

  override def start(): Unit =
    ensureConsumerGroup("$")
      .recover: e =>
        logError(s"Group init failed: ${e.getMessage}")
      .onComplete: _ =>
        vertx.setPeriodic(POLL_INTERVAL_MS, _ => poll())
        vertx.setPeriodic(CLAIM_CHECK_INTERVAL_MS, _ => reclaim())

  private[bot] def ensureConsumerGroup(startId: String): Future[Unit] =
    redisClient
      .send(
        Request
          .cmd(Command.XGROUP)
          .arg("CREATE")
          .arg(ChannelsKeys.BOT_TASKS_STREAM)
          .arg(ChannelsKeys.BOT_CONSUMER_GROUP)
          .arg(startId)
          .arg("MKSTREAM")
      )
      .asScala
      .void
      .recoverWith:
        case err if isBusyGroupError(err) => Future.unit

  private def recoverMissingGroup(op: String): PartialFunction[Throwable, Future[Unit]] =
    case err if isNoGroupError(err) =>
      logWarn(s"Consumer group missing during $op, recreating stream group from 0")
      ensureConsumerGroup("0").recover: e =>
        logError(s"Consumer group recreation failed during $op: ${e.getMessage}")
    case err =>
      logError(s"Redis $op failed: ${err.getMessage}")
      Future.unit

  private[bot] def poll(): Future[Unit] =
    redisClient
      .send(
        Request
          .cmd(Command.XREADGROUP)
          .arg("GROUP")
          .arg(ChannelsKeys.BOT_CONSUMER_GROUP)
          .arg(consumerId)
          .arg("COUNT")
          .arg(BATCH_SIZE.toString)
          .arg("STREAMS")
          .arg(ChannelsKeys.BOT_TASKS_STREAM)
          .arg(">")
      )
      .asScala
      .map(resp => if resp != null then dispatch(resp))
      .recoverWith(recoverMissingGroup("poll"))

  private[bot] def reclaim(): Future[Unit] =
    redisClient
      .send(
        Request
          .cmd(Command.XAUTOCLAIM)
          .arg(ChannelsKeys.BOT_TASKS_STREAM)
          .arg(ChannelsKeys.BOT_CONSUMER_GROUP)
          .arg(consumerId)
          .arg(CLAIM_IDLE_MS.toString)
          .arg("0-0")
          .arg("COUNT")
          .arg(BATCH_SIZE.toString)
      )
      .asScala
      .map: resp =>
        if resp != null && resp.get(1) != null then resp.get(1).asScala.foreach(processEntry)
      .recoverWith(recoverMissingGroup("reclaim"))

  private def dispatch(resp: Response): Unit =
    val entries =
      try resp.get(ChannelsKeys.BOT_TASKS_STREAM)
      catch case _: Exception => Option(resp.get(0)).map(_.get(1)).orNull
    if entries != null then entries.asScala.foreach(processEntry)

  private def processEntry(entry: Response): Unit =
    try
      val entryId = entry.get(0).toString
      val fields = entry.get(1)
      val dataJson =
        try fields.get(1).toString
        catch case _: Exception => fields.get("data").toString
      dataJson.decodeAs[BotTask] match
        case Right(task) => processTask(entryId, task)
        case Left(_)     => ackEntry(entryId)
    catch case e: Exception => logError(s"Entry parse failed: ${e.getMessage}")

  private[bot] def computeDelay(inv: InvitationEvent): Int = inv match
    case c: InvitationEvent.WaitingForCard if c.isTableEmpty =>
      DEFAULT_BOT_DELAY_MS + TRICK_RECAP_DELAY_MS
    case b: InvitationEvent.WaitingForBid if b.isFirstBid =>
      DEFAULT_BOT_DELAY_MS + TRICK_RECAP_DELAY_MS
    case _: InvitationEvent.WaitingForTrump =>
      DEFAULT_BOT_DELAY_MS + TRICK_RECAP_DELAY_MS
    case _ => DEFAULT_BOT_DELAY_MS

  private def processTask(entryId: String, task: BotTask): Unit =
    task.invitation match
      case inv: InvitationEvent => dispatchBotInvitation(entryId, task.lobbyId, inv)
      case _                    => ackEntry(entryId)

  private def dispatchBotInvitation(
      entryId: String,
      lobbyId: LobbyId,
      inv: InvitationEvent
  ): Unit =
    val actionFut =
      (for
        lobby <- OptionT(lobbyStatePort.getLobby(lobbyId).map(_.toOption))
        player <- OptionT.fromOption[Future](lobby.players.find(_.id == inv.destinationId))
        diff <- OptionT.fromOption[Future](player.difficulty)
        action <- OptionT.liftF(BotStrategy(diff, prologPort).resolveInvitation(lobbyId, inv))
      yield action).value

    actionFut.onComplete:
      case Success(Some(action)) =>
        vertx.setTimer(
          computeDelay(inv),
          _ => submitAndAck(lobbyId, inv, action, entryId)
        )
      case Success(None) =>
        ackEntry(entryId)
      case Failure(e) =>
        logError(s"Strategy failed: ${e.getMessage}")
        ackEntry(entryId)

  private def submitAndAck(
      lobbyId: LobbyId,
      inv: InvitationEvent,
      action: GameAction,
      entryId: String
  ): Unit =
    val submissionFut =
      for
        lobbyEither <- lobbyStatePort.getLobby(lobbyId)
        isBot = lobbyEither.toOption
          .flatMap(_.players.find(_.id == inv.destinationId))
          .exists(_.isBot)
        _ <-
          if isBot then submitWithFallback(lobbyId, inv, action)
          else Future.unit
      yield ()

    submissionFut.onComplete(_ => ackEntry(entryId))

  private def submitWithFallback(
      lobbyId: LobbyId,
      inv: InvitationEvent,
      action: GameAction
  ): Future[Unit] =
    gameInboundPort
      .submitAction(lobbyId, action)
      .flatMap:
        case Right(_) => Future.unit
        case Left(err) =>
          logWarn(s"Action failed for bot ${inv.destinationId} ($err). Forcing fallback.")
          gameInboundPort.forceFallbackAction(lobbyId, inv.destinationId)

  private def ackEntry(entryId: String): Unit =
    redisClient
      .send(
        Request
          .cmd(Command.XACK)
          .arg(ChannelsKeys.BOT_TASKS_STREAM)
          .arg(ChannelsKeys.BOT_CONSUMER_GROUP)
          .arg(entryId)
      )
      .onComplete(_ => ())

  private def logError(msg: String): Unit =
    WizardLogger.error(msg)

  private def logWarn(msg: String): Unit =
    WizardLogger.warn(msg)

object BotManagerVerticle:
  private def DEFAULT_BOT_DELAY_MS: Int = Random.between(800, 2000)
  private val TRICK_RECAP_DELAY_MS: Int = 5000
  private val POLL_INTERVAL_MS: Long = 500L
  private val CLAIM_CHECK_INTERVAL_MS: Long = 10_000L
  private val CLAIM_IDLE_MS: Long = 15_000L
  private val BATCH_SIZE: Int = 10

  private[bot] def isNoGroupError(err: Throwable): Boolean =
    Option(err.getMessage).exists(_.contains("NOGROUP"))

  private[bot] def isBusyGroupError(err: Throwable): Boolean =
    Option(err.getMessage).exists(_.contains("BUSYGROUP"))
