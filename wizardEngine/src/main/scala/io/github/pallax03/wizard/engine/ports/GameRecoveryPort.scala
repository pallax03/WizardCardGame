package io.github.pallax03.wizard.engine.ports

import scala.concurrent.Future

import io.github.pallax03.wizard.engine.lobby.LobbyId
import io.github.pallax03.wizard.engine.model.core.GameException

/**
 * Fault-tolerance port for checkpoint-based state recovery upon fatal engine failures.
 *
 * Implements an escalation strategy: when corrupted or inconsistent states trigger
 * a [[GameException]], the system attempts to restore the most recent consistent checkpoint.
 */
trait GameRecoveryPort:

  /**
   * Attempts to roll back game state to the latest valid checkpoint.
   *
   * @return future completing with:
   *  - `true` if state was successfully restored to a valid checkpoint and play can continue.
   *  - `false` if checkpoints are absent or corrupted, triggering an unrecoverable game abort.
   */
  def attemptRecovery(lobbyId: LobbyId, exception: GameException): Future[Boolean]
