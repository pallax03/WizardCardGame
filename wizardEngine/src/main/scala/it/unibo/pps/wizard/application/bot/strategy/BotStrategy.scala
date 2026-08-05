package it.unibo.pps.wizard.application.bot.strategy

import it.unibo.pps.wizard.engine.events.FailureEvent
import it.unibo.pps.wizard.engine.events.InvitationEvent
import it.unibo.pps.wizard.engine.model.configuration.BotsDifficulty
import it.unibo.pps.wizard.engine.model.core.GameAction
import it.unibo.pps.wizard.engine.ports.AIPort

import scala.concurrent.Future

/**
 * Defines the strategy interface for AI-controlled players.
 *
 * This allows the system to support different levels of intelligence.
 */
trait BotStrategy:
  /**
   * Processes an invitation event to decide the bot's next move.
   *
   * @param invitation the specific invitation event (e.g., bid, play card).
   * @return A [[Future]] containing the calculated [[GameAction]].
   */
  def resolveInvitationEvents(invitation: InvitationEvent): Future[GameAction]

  /**
   * Handles failure events to attempt a recovery or a fallback action.
   *
   * @param failure the error event describing the failed action.
   * @return A [[Future]] containing the recovery [[GameAction]].
   */
  def resolveFailedEvents(failure: FailureEvent): Future[GameAction]

object BotStrategy:
  /**
   * Factory method to instantiate the appropriate [[BotStrategy]] based on
   * the selected difficulty.
   *
   * @param difficulty   The chosen [[BotsDifficulty]].
   * @param wizardAIPort The port used by [[PrologBotStrategy]], or a new future AI supported strategy.
   * @return A concrete implementation of [[BotStrategy]].
   */
  def apply(difficulty: BotsDifficulty, wizardAIPort: AIPort): BotStrategy = difficulty match
    case BotsDifficulty.Dumb   => new DumbBotStrategy()
    case BotsDifficulty.Prolog => new PrologBotStrategy(wizardAIPort)
