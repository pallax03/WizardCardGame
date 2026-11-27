package io.github.pallax03.wizard.util

import scala.jdk.CollectionConverters.*

import ch.qos.logback.classic.joran.JoranConfigurator
import ch.qos.logback.classic.{AsyncAppender, Level, Logger as LogbackLogger, LoggerContext}
import ch.qos.logback.core.status.Status
import org.slf4j.MDC

import org.scalatest.concurrent.Eventually
import org.scalatest.matchers.should.Matchers
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatest.wordspec.AnyWordSpec

class TestWizardLogger extends AnyWordSpec with Matchers with Eventually:

  given PatienceConfig = PatienceConfig(timeout = Span(5, Seconds), interval = Span(25, Millis))

  "Logback configuration" should:
    "initialize without status errors and route root logging through AsyncAppender" in:
      val context = new LoggerContext()
      try
        val configurator = new JoranConfigurator()
        configurator.setContext(context)
        configurator.doConfigure(getClass.getResource("/logback.xml"))

        val errors = context.getStatusManager.getCopyOfStatusList.asScala
          .filter(_.getLevel == Status.ERROR)
        errors shouldBe empty

        val root = context.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME)
        val asyncAppender = root.getAppender("ASYNC").asInstanceOf[AsyncAppender]
        asyncAppender should not be null
        asyncAppender.getQueueSize shouldBe 512
        asyncAppender.isNeverBlock shouldBe true
      finally context.stop()

  "WizardLogger" should:
    "resolve the caller class name via StackWalker" in:
      WizardLogger.callerLogger.getName shouldBe getClass.getName

    "inject and clean up MDC context even when the block throws" in:
      WizardLogger.withContext(Some("lobby-42"), Some("player-7")):
        MDC.get("lobbyId") shouldBe "lobby-42"
        MDC.get("playerId") shouldBe "player-7"
      MDC.get("lobbyId") shouldBe null
      MDC.get("playerId") shouldBe null

      intercept[IllegalStateException]:
        WizardLogger.withContext(Some("lobby-err"), Some("player-err")):
          throw new IllegalStateException("boom")
      MDC.get("lobbyId") shouldBe null
      MDC.get("playerId") shouldBe null

    "evaluate debug messages lazily only when debug logging is enabled" in:
      val logbackLogger = eventually:
        WizardLogger.callerLogger match
          case l: LogbackLogger => l
          case other            => fail(s"SLF4J still initializing (got ${other.getClass.getName})")
      val previousLevel = logbackLogger.getLevel
      try
        logbackLogger.setLevel(Level.INFO)
        var evaluatedAtInfo = false
        WizardLogger.debug {
          evaluatedAtInfo = true
          "should not be evaluated"
        }
        evaluatedAtInfo shouldBe false

        logbackLogger.setLevel(Level.DEBUG)
        var evaluatedAtDebug = false
        WizardLogger.debug {
          evaluatedAtDebug = true
          "should be evaluated"
        }
        evaluatedAtDebug shouldBe true
      finally logbackLogger.setLevel(previousLevel)
