package io.github.pallax03.wizard.application.bot

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.jdk.CollectionConverters.*
import scala.util.{Failure, Random, Success}

import cats.syntax.all.*

import io.vertx.core.AbstractVerticle
import io.vertx.redis.client.{Command, Redis, Request, Response}

import io.github.pallax03.wizard.application.bot.strategy.BotStrategy
import io.github.pallax03.wizard.codecs.engine.lobby.BotTaskCodecs.given
import io.github.pallax03.wizard.codecs.syntax.CodecSyntax.*
import io.github.pallax03.wizard.engine.lobby.{BotTask, LobbyId}
import io.github.pallax03.wizard.engine.model.core.GameAction
import io.github.pallax03.wizard.engine.model.events.InvitationEvent
import io.github.pallax03.wizard.engine.model.rules.FallbackStrategy
import io.github.pallax03.wizard.engine.ports.*
import io.github.pallax03.wizard.util.FutureSyntax.*
import io.github.pallax03.wizard.util.{ChannelsKeys, WizardLogger}

class BotManagerVerticle(
    prologPort: AIPort,
    lobbyStatePort: LobbyStatePort,
    gameInboundPort: InboundPort,
    redisClient: Redis
) extends AbstractVerticle:

  import BotManagerVerticle.*
  private val consumerId = s"bot-${java.util.UUID.randomUUID()}"

  override def start(): Unit =
    redisClient
      .send(
        Request
          .cmd(Command.XGROUP)
          .arg("CREATE")
          .arg(ChannelsKeys.BOT_TASKS_STREAM)
          .arg(ChannelsKeys.BOT_CONSUMER_GROUP)
          .arg("$")
          .arg("MKSTREAM")
      )
      .asScala
      .void
      .recover { case _ => () }
      .onComplete:
        case Success(_) =>
          vertx.setPeriodic(POLL_INTERVAL_MS, _ => poll())
          vertx.setPeriodic(CLAIM_CHECK_INTERVAL_MS, _ => reclaim())
        case Failure(e) => logError(s"Group init failed: ${e.getMessage}")

  private def poll(): Unit =
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
      .foreach(resp => if resp != null then dispatch(resp))

  private def reclaim(): Unit =
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
      .foreach(resp =>
        if resp != null && resp.get(1) != null then resp.get(1).asScala.foreach(processEntry)
      )

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

  private def processTask(entryId: String, task: BotTask): Unit =
    task.invitation match
      case inv: InvitationEvent =>
        lobbyStatePort
          .getLobby(task.lobbyId)
          .onComplete:
            case Success(Right(lobby)) =>
              lobby.players.find(_.id == inv.destinationId).flatMap(_.difficulty) match
                case Some(diff) =>
                  val strat = BotStrategy(diff, prologPort)
                  strat
                    .resolveInvitation(task.lobbyId, inv)
                    .onComplete:
                      case Success(action) =>
                        val delay = inv match
                          case c: InvitationEvent.WaitingForCard if c.isTableEmpty =>
                            DEFAULT_BOT_DELAY_MS + FIRST_CARD_DELAY_MS
                          case _ => DEFAULT_BOT_DELAY_MS
                        vertx.setTimer(
                          delay,
                          _ => submitAndAck(task.lobbyId, inv, action, entryId)
                        )
                      case Failure(e) =>
                        logError(s"Strategy failed: ${e.getMessage}"); ackEntry(entryId)
                case None => ackEntry(entryId)
            case _ => ackEntry(entryId)
      case _ => ackEntry(entryId)

  private def submitAndAck(
      lobbyId: LobbyId,
      inv: InvitationEvent,
      action: GameAction,
      entryId: String
  ): Unit =
    lobbyStatePort
      .getLobby(lobbyId)
      .onComplete:
        case Success(Right(lobby))
            if lobby.players.find(_.id == inv.destinationId).exists(_.isBot) =>
          gameInboundPort
            .submitAction(lobbyId, action)
            .flatMap:
              case Right(_) => Future.unit
              case Left(err) =>
                logWarn(s"Action failed for bot ${inv.destinationId} ($err). Forcing fallback.")
                gameInboundPort.submitAction(lobbyId, FallbackStrategy.fallbackMove(inv)).void
            .onComplete(_ => ackEntry(entryId))
        case _ => ackEntry(entryId)

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
    WizardLogger.error(s"[BotManager] $msg")

  private def logWarn(msg: String): Unit =
    WizardLogger.warn(s"[BotManager] $msg")

object BotManagerVerticle:
  private def DEFAULT_BOT_DELAY_MS: Int = Random.between(800, 2000)
  private def FIRST_CARD_DELAY_MS: Int = 5000
  private val POLL_INTERVAL_MS: Long = 500L
  private val CLAIM_CHECK_INTERVAL_MS: Long = 10_000L
  private val CLAIM_IDLE_MS: Long = 15_000L
  private val BATCH_SIZE: Int = 10
