package io.github.pallax03.wizard.util

import io.github.pallax03.wizard.engine.lobby.LobbyId
import io.github.pallax03.wizard.engine.model.basic.PlayerId

import org.slf4j.{Logger, LoggerFactory, MDC}

/** Context carrier holding distributed tracing identifiers (`lobbyId`, `playerId`) for logging. */
case class LogContext(lobbyId: Option[String] = None, playerId: Option[String] = None)

object LogContext:
  def apply(lobbyId: LobbyId): LogContext = new LogContext(Some(lobbyId.toString), None)
  def apply(lobbyId: LobbyId, playerId: PlayerId): LogContext =
    new LogContext(Some(lobbyId.toString), Some(playerId.toString))

/**
 * Structured logging utility with automated Mapped Diagnostic Context (MDC) enrichment.
 *
 * Injects `lobbyId` and `playerId` into MDC for log correlation and guarantees thread-local
 * cleanup in `finally` blocks to prevent context leakage across Vert.x event loop threads.
 */
object WizardLogger:
  private val walker = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE)
  private val selfPrefix = getClass.getName.stripSuffix("$")

  private[util] def callerLogger: Logger =
    val callerName = walker.walk: frames =>
      frames
        .map(_.getDeclaringClass.getName.stripSuffix("$"))
        .filter(!_.startsWith(selfPrefix))
        .findFirst()
        .orElse(selfPrefix)
    LoggerFactory.getLogger(callerName)

  def debug(msg: => String)(using ctx: LogContext = LogContext()): Unit =
    val logger = callerLogger
    if logger.isDebugEnabled then withMDC(ctx)(logger.debug(msg))

  def info(msg: String)(using ctx: LogContext = LogContext()): Unit =
    withMDC(ctx)(callerLogger.info(msg))

  def warn(msg: String)(using ctx: LogContext = LogContext()): Unit =
    withMDC(ctx)(callerLogger.warn(msg))

  def error(msg: String, cause: Throwable = null)(using ctx: LogContext = LogContext()): Unit =
    withMDC(ctx):
      val logger = callerLogger
      if cause == null then logger.error(msg)
      else logger.error(msg, cause)

  private def withMDC[A](ctx: LogContext)(block: => A): A =
    ctx.lobbyId.foreach(id => MDC.put("lobbyId", id))
    ctx.playerId.foreach(id => MDC.put("playerId", id))
    try block
    finally
      ctx.lobbyId.foreach(_ => MDC.remove("lobbyId"))
      ctx.playerId.foreach(_ => MDC.remove("playerId"))

  def withContext[A](lobbyId: Option[String] = None, playerId: Option[String] = None)(
      block: => A
  ): A =
    withMDC(LogContext(lobbyId, playerId))(block)
