package it.unibo.pps.wizard.engine.ports

import it.unibo.pps.wizard.engine.lobby.LobbyId
import it.unibo.pps.wizard.engine.model.basic.PlayerId
import it.unibo.pps.wizard.engine.model.basic.bidding.Bid
import it.unibo.pps.wizard.engine.model.basic.cards.Card

import scala.concurrent.Future

/**
 * Defines the interface for an AI component capable of making decisions within the Wizard game.
 *
 * Implementations of this port are responsible for querying game logic
 * to provide valid actions based on the current [[GameState]].
 *
 * Each method is asynchronous, returning a [[Future]] to ensure the game engine
 * remains responsive while the AI computes its strategy.
 */
trait AIPort:

  /**
   * Selects the best trump color to resolve a Wizard card.
   *
   * @param lobbyId the ID of the lobby.
   * @param playerId the ID of the dealer who needs to resolve the trump.
   * @return A [[Future]] containing the chosen [[Card.Color]].
   */
  def resolvedTrumpColor(lobbyId: LobbyId, playerId: PlayerId): Future[Card.Color]

  /**
   * Determines the bid for the current round.
   *
   * @param lobbyId the ID of the lobby.
   * @param playerId the ID of the player placing the bid.
   * @return A [[Future]] containing the suggested [[Bid]].
   */
  def placeBid(lobbyId: LobbyId, playerId: PlayerId): Future[Bid]

  /**
   * Adjusts a previously rejected bid.
   *
   * @param lobbyId the ID of the lobby.
   * @param playerId the ID of the player adjusting the bid.
   * @return A [[Future]] containing the adjusted [[Bid]] that satisfies game constraints.
   */
  def adjustBid(lobbyId: LobbyId, playerId: PlayerId): Future[Bid]

  /**
   * Selects the optimal card to play from the player's hand given the current table state.
   *
   * @param lobbyId the ID of the lobby.
   * @param playerId the ID of the player whose turn it is.
   * @return A [[Future]] containing the selected [[Card]].
   */
  def bestCard(lobbyId: LobbyId, playerId: PlayerId): Future[Card]
