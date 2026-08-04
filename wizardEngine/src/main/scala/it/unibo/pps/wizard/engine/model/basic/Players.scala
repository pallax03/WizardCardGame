package it.unibo.pps.wizard.engine.model.basic

import it.unibo.pps.wizard.engine.model.basic.cards.Card

/** Represents the unique identifier of a player in the game. */
opaque type PlayerId = Int

object PlayerId:
  def apply(s: Int): PlayerId = s

  extension (p: PlayerId) infix def plays(c: Card): (PlayerId, Card) = (p, c)

/** Represents the name of a player in the game. */
opaque type PlayerName = String

object PlayerName:
  def apply(s: String): PlayerName = s

/** Represents a player in the game, including their ID, name, and whether they are a bot. */
final case class Player(id: PlayerId, name: PlayerName, isBot: Boolean)

object Player:
  def human(id: PlayerId, name: PlayerName): Player = Player(id, name, isBot = false)
  def bot(id: PlayerId): Player = Player(id, PlayerName(s"Bot $id"), isBot = true)

/** Represents a collection of players in the game. */
opaque type Players = List[Player]

object Players:
  def apply(players: Player*): Players = players.toList
  def create(players: Players, numberOfComputers: Int): Players =
    players ++ generateComputers(numberOfComputers)
  private def generateComputers(numberOfComputers: Int): Players =
    (1 to numberOfComputers).map(id => Player.bot(PlayerId(id))).toList

  extension (players: Players)
    def toList: List[Player] = players
    def getPlayerIds: List[PlayerId] = players.map(_.id)
    def totalPlayers: Int = players.size
    def filter(predicate: Player => Boolean): Players = players.filter(predicate)
    def findById(id: PlayerId): Option[Player] = players.find(_.id == id)
    def getPlayersNames: List[PlayerName] = players.map(_.name)
