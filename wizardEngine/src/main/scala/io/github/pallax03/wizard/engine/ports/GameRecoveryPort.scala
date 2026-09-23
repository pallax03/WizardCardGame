package io.github.pallax03.wizard.engine.ports

import scala.concurrent.Future

import io.github.pallax03.wizard.engine.lobby.LobbyId
import io.github.pallax03.wizard.engine.model.core.GameException

trait GameRecoveryPort:
  /**
   * Attempts to recover from a fatal GameException using a Checkpoint-Based Escalation strategy.
   *
   * @return Future(true) if the state was successfully rolled back to a checkpoint.
   *         Future(false) if the checkpoint was missing or corrupted, resulting in Game Abort.
   */
  def attemptRecovery(lobbyId: LobbyId, exception: GameException): Future[Boolean]
