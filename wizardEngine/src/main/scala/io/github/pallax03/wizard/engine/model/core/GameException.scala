package io.github.pallax03.wizard.engine.model.core

import io.github.pallax03.wizard.engine.model.basic.PlayerId

/**
 * Unchecked exceptions representing fatal internal engine inconsistencies or corrupted state.
 *
 * Unlike [[GameActionError]] (which represents normal domain rule rejections of client moves),
 * instances of [[GameException]] indicate illegal internal states, missing data, or bugs in the engine workflow.
 */
enum GameException(message: String) extends Exception(message):
  case TableNoWinner extends GameException("Table has no winner")
  case CorruptedHand(playerId: PlayerId)
      extends GameException(s"Corrupted hand for player $playerId")
  case CorruptedState(msg: String) extends GameException(s"Corrupted state: $msg")

case class RecoveredGameException(ge: GameException) extends Exception(ge.getMessage, ge)
case class AbortedGameException(ge: GameException) extends Exception(ge.getMessage, ge)
