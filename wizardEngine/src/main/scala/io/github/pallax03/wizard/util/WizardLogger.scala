package io.github.pallax03.wizard.util

import org.slf4j.{LoggerFactory, MDC}

object WizardLogger:
  private val logger = LoggerFactory.getLogger("WizardApp")

  def info(msg: String): Unit = logger.info(msg)

  def warn(msg: String): Unit = logger.warn(msg)

  def error(msg: String, cause: Throwable): Unit = logger.error(msg, cause)

  def error(msg: String): Unit = logger.error(msg)

  def withContext[A](lobbyId: Option[String] = None, playerId: Option[String] = None)(
      block: => A
  ): A =
    lobbyId.foreach(id => MDC.put("lobbyId", id))
    playerId.foreach(id => MDC.put("playerId", id))
    try block
    finally
      lobbyId.foreach(_ => MDC.remove("lobbyId"))
      playerId.foreach(_ => MDC.remove("playerId"))
