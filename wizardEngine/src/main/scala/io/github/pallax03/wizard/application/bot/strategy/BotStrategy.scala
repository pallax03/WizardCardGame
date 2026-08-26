package io.github.pallax03.wizard.application.bot.strategy

import io.github.pallax03.wizard.engine.lobby.BotsDifficulty
import io.github.pallax03.wizard.engine.lobby.LobbyId
import io.github.pallax03.wizard.engine.model.core.GameAction
import io.github.pallax03.wizard.engine.model.events.FailureEvent
import io.github.pallax03.wizard.engine.model.events.InvitationEvent
import io.github.pallax03.wizard.engine.ports.AIPort

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
   * @param lobbyId the identifier of the lobby.
   * @param invitation the specific invitation event (e.g., bid, play card).
   * @return A [[Future]] containing the calculated [[GameAction]].
   */
  def resolveInvitationEvents(lobbyId: LobbyId, invitation: InvitationEvent): Future[GameAction]

  /**
   * Handles failure events to attempt a recovery or a fallback action.
   *
   * @param lobbyId the identifier of the lobby.
   * @param failure the error event describing the failed action.
   * @return A [[Future]] containing the recovery [[GameAction]].
   */
  def resolveFailedEvents(lobbyId: LobbyId, failure: FailureEvent): Future[GameAction]

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
