package it.unibo.pps.wizard.engine.model.basic.players

import it.unibo.pps.wizard.engine.model.basic.Player
import it.unibo.pps.wizard.engine.model.basic.PlayerId
import it.unibo.pps.wizard.engine.model.basic.PlayerName
import it.unibo.pps.wizard.engine.model.basic.Players
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class TestPlayers extends AnyWordSpec with Matchers:
  "A Player" should:
    val id: PlayerId = PlayerId(1)
    val name: PlayerName = PlayerName("Alice")
    val p = Player.human(id, name)
    "have the correct id" in:
      p.id shouldBe id
      p.isBot shouldBe false

  "A Bot" should:
    val id: PlayerId = PlayerId(2)
    val com = Player.bot(id)
    "have the correct id" in:
      com.id shouldBe id
      com.isBot shouldBe true

  "Players" should:
    val p1 = Player.human(PlayerId(1), PlayerName("Alice"))
    val p2 = Player.human(PlayerId(2), PlayerName("Bob"))
    val players = Players(p1, p2)

    "be created with a list of players" in:
      players.toList should contain theSameElementsAs List(p1, p2)

    "be created with a list of players and bots" in:
      val playersAndBots = Players.create(players, 2)
      playersAndBots.toList.size shouldBe 4

    "be filtered correctly according to a predicate" in:
      val filtered = players.filter(_.name == PlayerName("Alice"))
      filtered.toList should contain theSameElementsAs List(p1)

    "retrieve all player names as a list" in:
      players.getPlayersNames should contain theSameElementsAs List(
        PlayerName("Alice"),
        PlayerName("Bob")
      )
