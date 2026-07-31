package it.unibo.pps.wizard.engine.model.core

import it.unibo.pps.wizard.engine.model.basic._
import it.unibo.pps.wizard.engine.model.basic.bidding.Bid
import it.unibo.pps.wizard.engine.model.basic.cards.Card

/**
 * Represents an explicit command or intention submitted by a player.
 * GameActions are the only acceptable inputs that the [[GameEngine]] can process to advance the state of the game.
 * They represent attempts to alter the game state and can be rejected (as [[GameError]]) if they violate game rules.
 */
enum GameAction:
  def playerId: PlayerId

  case ResolveTrumpColor(playerId: PlayerId, color: Card.Color)
  case PlaceBid(playerId: PlayerId, bid: Bid)
  case PlayCard(playerId: PlayerId, card: Card)
