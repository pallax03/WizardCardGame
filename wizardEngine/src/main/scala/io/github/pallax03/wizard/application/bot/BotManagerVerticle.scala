package io.github.pallax03.wizard.application.bot


import scala.concurrent.ExecutionContext.Implicits.global
import scala.jdk.CollectionConverters.*
import scala.util.{Failure, Success}

import io.vertx.core.AbstractVerticle
import io.vertx.redis.client.{Command, Redis, Request, Response}

import io.github.pallax03.wizard.application.bot.strategy.BotStrategy
import io.github.pallax03.wizard.codecs.engine.lobby.BotTaskCodecs.given
import io.github.pallax03.wizard.codecs.syntax.CodecSyntax.*
import io.github.pallax03.wizard.engine.lobby.BotTask
import io.github.pallax03.wizard.engine.model.basic.PlayerId
import io.github.pallax03.wizard.engine.model.core.GameAction
import io.github.pallax03.wizard.engine.model.events.{FailureEvent, InvitationEvent}
import io.github.pallax03.wizard.engine.ports.*
import io.github.pallax03.wizard.util.ChannelsKeys
import io.github.pallax03.wizard.util.FutureSyntax.*

import scala.concurrent.Future


/**
 * Stateless bot worker — consumes [[BotTask]]s from the Redis Stream `bot:tasks`.
 *
 * Each stream entry carries a single `data` field: the JSON of a [[BotTask]].
 * Circe decodes it; no RESP2/RESP3 field-index parsing needed.
 *
 * Resilience: XAUTOCLAIM every [[CLAIM_CHECK_INTERVAL_MS]] reclaims entries stuck
 * in the PEL (pending entry list) after a pod crash during [[botDelayMs]].
 */
class BotManagerVerticle(
    pubSubPort: PubSubPort,
    prologPort: AIPort,
    lobbyStatePort: LobbyStatePort,
    gameInboundPort: InboundPort,
    redisClient: Redis,
    botDelayMs: Long = BotManagerVerticle.DEFAULT_BOT_DELAY_MS
) extends AbstractVerticle:

  import BotManagerVerticle.*

  private val consumerId = s"bot-${java.util.UUID.randomUUID()}"

  override def start(): Unit =
    ensureConsumerGroup().onComplete:
      case Success(_) =>
        vertx.setPeriodic(POLL_INTERVAL_MS, _ => poll())
        vertx.setPeriodic(CLAIM_CHECK_INTERVAL_MS, _ => reclaim())
      case Failure(e) =>
        log(s"ERROR:[BotManager] group init failed: ${e.getMessage}")

  // ---------------------------------------------------------------------------
  // Stream bootstrap
  // ---------------------------------------------------------------------------

  private def ensureConsumerGroup(): Future[Unit] =
    redisClient
      .send(
        Request.cmd(Command.XGROUP)
          .arg("CREATE").arg(ChannelsKeys.BOT_TASKS_STREAM)
          .arg(ChannelsKeys.BOT_CONSUMER_GROUP).arg("$").arg("MKSTREAM")
      )
      .asScala.map(_ => ())
      .recover { case e if e.getMessage.contains("BUSYGROUP") => () }

  // ---------------------------------------------------------------------------
  // Poll + reclaim
  // ---------------------------------------------------------------------------

  private def poll(): Unit =
    redisClient
      .send(
        Request.cmd(Command.XREADGROUP)
          .arg("GROUP").arg(ChannelsKeys.BOT_CONSUMER_GROUP).arg(consumerId)
          .arg("COUNT").arg(BATCH_SIZE.toString)
          .arg("STREAMS").arg(ChannelsKeys.BOT_TASKS_STREAM).arg(">")
      )
      .asScala
      .onComplete:
        case Success(resp) if resp != null => dispatch(resp)
        case _                             => ()

  private def reclaim(): Unit =
    redisClient
      .send(
        Request.cmd(Command.XAUTOCLAIM)
          .arg(ChannelsKeys.BOT_TASKS_STREAM).arg(ChannelsKeys.BOT_CONSUMER_GROUP)
          .arg(consumerId).arg(CLAIM_IDLE_MS.toString).arg("0-0")
          .arg("COUNT").arg(BATCH_SIZE.toString)
      )
      .asScala
      .onComplete:
        case Success(resp) if resp != null =>
          // XAUTOCLAIM: [nextId, entries, deletedIds] — entries at index 1
          val entries = resp.get(1)
          if entries != null then entries.asScala.foreach(processEntry)
        case _ => ()

  // ---------------------------------------------------------------------------
  // Dispatch: extract entries from XREADGROUP response
  //
  // The raw response is one JSON string in the single "data" field — so we only
  // need to reach field index 1 of the first entry, regardless of protocol.
  // XREADGROUP wraps entries as: resp[stream][entries][entry][id/fields]
  // ponytail: we iterate with asScala and rely on the typed BotTask decode
  //           to reject any malformed/unexpected entries gracefully.
  // ---------------------------------------------------------------------------

  private def dispatch(resp: Response): Unit =
    val entries =
      try resp.get(ChannelsKeys.BOT_TASKS_STREAM) // RESP3 Map
      catch case _: Exception =>
        val block = resp.get(0) // RESP2 Array: [[streamName, entries]]
        if block != null then block.get(1) else null
    if entries != null then entries.asScala.foreach(processEntry)

  private def processEntry(entry: Response): Unit =
    try
      val entryId  = entry.get(0).toString
      val fields   = entry.get(1)
      // single field "data" at index 1 (RESP2) or by key "data" (RESP3)
      val dataJson =
        try fields.get(1).toString          // RESP2: flat array [key, value]
        catch case _: Exception => fields.get("data").toString // RESP3: map
      dataJson.decodeAs[BotTask] match
        case Right(task) => processTask(entryId, task)
        case Left(err) =>
          log(s"WARN:[BotManager] Decode failed for entry $entryId: $err. JSON: $dataJson")
          ackEntry(entryId) // malformed: drop
    catch case e: Exception =>
      log(s"ERROR:[BotManager] entry parse failed: ${e.getMessage}")


  // ---------------------------------------------------------------------------
  // Task execution
  // ---------------------------------------------------------------------------

  private def processTask(entryId: String, task: BotTask): Unit =
    task.invitation match
      case inv: InvitationEvent =>
        lobbyStatePort.getLobby(task.lobbyId).onComplete:
          case Success(Some(lobby)) =>
            lobby.players.find(_.id == inv.destinationId).flatMap(_.difficulty) match
              case Some(diff) =>
                log(s"INFO:[BotManager] Executing task $entryId for bot ${inv.destinationId} in lobby ${task.lobbyId} (delay: ${botDelayMs}ms)")
                val strat = BotStrategy(diff, prologPort)
                strat.resolveInvitationEvents(task.lobbyId, inv).onComplete:
                  case Failure(e) =>
                    log(s"ERROR:[BotManager] strategy failed: ${e.getMessage}")
                    ackEntry(entryId)
                  case Success(action) =>
                    vertx.setTimer(botDelayMs, _ => submitAndAck(task.lobbyId, inv.destinationId, strat, action, entryId))
              case None =>
                log(s"WARN:[BotManager] Task $entryId skipped: player ${inv.destinationId} not found or not a bot")
                ackEntry(entryId)
          case _ =>
            log(s"WARN:[BotManager] Task $entryId skipped: lobby ${task.lobbyId} not found")
            ackEntry(entryId)
      case _ =>
        log(s"WARN:[BotManager] Task $entryId skipped: not an InvitationEvent")
        ackEntry(entryId)


  private def submitAndAck(
      lobbyId: io.github.pallax03.wizard.engine.lobby.LobbyId,
      playerId: PlayerId,
      strategy: BotStrategy,
      action: GameAction,
      entryId: String
  ): Unit =
    gameInboundPort
      .submitAction(lobbyId, action)
      .flatMap:
        case Right(_)  =>
          log(s"INFO:[BotManager] Action submitted for bot $playerId in lobby $lobbyId")
          Future.unit
        case Left(err) =>
          log(s"WARN:[BotManager] Action failed for bot $playerId ($err). Trying fallback...")
          strategy.resolveFailedEvents(lobbyId, FailureEvent.ActionFailed(playerId, err))
            .flatMap(fallback => gameInboundPort.submitAction(lobbyId, fallback).map(_ => ()))
      .onComplete(_ => ackEntry(entryId))

  private def ackEntry(entryId: String): Unit =
    redisClient.send(
      Request.cmd(Command.XACK)
        .arg(ChannelsKeys.BOT_TASKS_STREAM)
        .arg(ChannelsKeys.BOT_CONSUMER_GROUP)
        .arg(entryId)
    ).onComplete(_ => ())

  private def log(msg: String): Unit =
    pubSubPort.publish(ChannelsKeys.LOGS_CHANNEL, msg)

object BotManagerVerticle:
  val DEFAULT_BOT_DELAY_MS: Long    = 3_000L
  private val POLL_INTERVAL_MS: Long     = 500L
  private val CLAIM_CHECK_INTERVAL_MS: Long = 10_000L
  private val CLAIM_IDLE_MS: Long        = 15_000L
  private val BATCH_SIZE: Int            = 10
