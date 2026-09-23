package io.github.pallax03.wizard.util

import io.github.pallax03.wizard.engine.lobby.LobbyId
import io.github.pallax03.wizard.engine.model.basic.PlayerId

import org.slf4j.{LoggerFactory, MDC}

case class LogContext(lobbyId: Option[String] = None, playerId: Option[String] = None)

object LogContext:
  def apply(lobbyId: LobbyId): LogContext = new LogContext(Some(lobbyId.toString), None)
  def apply(lobbyId: LobbyId, playerId: PlayerId): LogContext =
    new LogContext(Some(lobbyId.toString), Some(playerId.toString))

object WizardLogger:
  private val logger = LoggerFactory.getLogger("WizardApp")

  def info(msg: String)(using ctx: LogContext = LogContext()): Unit =
    withMDC(ctx)(logger.info(msg))

  def warn(msg: String)(using ctx: LogContext = LogContext()): Unit =
    withMDC(ctx)(logger.warn(msg))

  def error(msg: String, cause: Throwable = null)(using ctx: LogContext = LogContext()): Unit =
    withMDC(ctx):
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
