package io.github.pallax03.wizard.engine.model.core

import io.github.pallax03.wizard.engine.model.basic.*
import io.github.pallax03.wizard.engine.model.basic.bidding.Bid
import io.github.pallax03.wizard.engine.model.basic.cards.Card

/**
 * Represents an explicit move or command submitted by a player to advance the game.
 *
 * Game actions are the only valid inputs accepted by [[GameEngine.processAction]].
 * If an action violates game rules or is played out of turn, it is rejected with a [[GameActionError]].
 *
 * @see [[GameEngine.processAction]] for action processing.
 * @see [[GameActionError]] for rejection reasons.
 */
enum GameAction:
  def playerId: PlayerId

  case ResolveTrumpColor(playerId: PlayerId, color: Card.Color)
  case PlaceBid(playerId: PlayerId, bid: Bid)
  case PlayCard(playerId: PlayerId, card: Card)
