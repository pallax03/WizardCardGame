package io.github.pallax03.wizard.application.timer

import io.vertx.redis.client.Command

import io.github.pallax03.wizard.codecs.engine.lobby.LobbyPlayerCodecs.given
import io.github.pallax03.wizard.codecs.engine.model.SystemEventCodecs.given
import io.github.pallax03.wizard.codecs.syntax.CodecSyntax.*
import io.github.pallax03.wizard.engine.adapters.redis.StubRedis
import io.github.pallax03.wizard.engine.lobby.*
import io.github.pallax03.wizard.engine.model.basic.PlayerId
import io.github.pallax03.wizard.engine.model.events.SystemEvent
import io.github.pallax03.wizard.engine.ports.{
  InMemoryLobbyStatePort,
  InMemoryPubSubPort,
  StubInboundPort
}
import io.github.pallax03.wizard.util.ChannelsKeys

import org.scalatest.OptionValues
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatest.wordspec.AnyWordSpec

class TestTurnTimerVerticle extends AnyWordSpec with Matchers with OptionValues with ScalaFutures:

  given PatienceConfig = PatienceConfig(timeout = Span(2, Seconds), interval = Span(15, Millis))

  "TurnTimerVerticle" when:
    val humanId = PlayerId(0)
    val botId = PlayerId(1)
    val otherBotId = PlayerId(2)

    def createHarness(
        lobby: Lobby
    ): (TurnTimerVerticle, InMemoryPubSubPort, StubRedis, StubInboundPort, InMemoryLobbyStatePort) =
      val pubSub = InMemoryPubSubPort()
      val redis = StubRedis()
      val inbound = StubInboundPort()
      val lobbyPort = InMemoryLobbyStatePort(lobby)
      val verticle = TurnTimerVerticle(pubSub, redis, inbound, lobbyPort)
      (verticle, pubSub, redis, inbound, lobbyPort)

    "arming turn timers" should:
      "use the dedicated 12s timeout for bots regardless of lobby timer configuration" in:
        val lobby = (human(humanId) - bot(botId) - bot(otherBotId))
          .asLobby(LobbyStatus.IN_GAME)
          .copy(configuration = GameConfiguration(timer = 90))
        val (verticle, _, redis, _, _) = createHarness(lobby)

        verticle.handleTurnEvent(LobbyPlayer(defaultLobbyId, botId).toJson).futureValue

        redis.commands shouldBe List(
          (
            Command.SET,
            List(
              ChannelsKeys.turnTimer(defaultLobbyId, botId),
              "1",
              "EX",
              GameConfiguration.BOT_TURN_TIMEOUT_SECONDS.toString
            )
          )
        )

      "use the dedicated 12s timeout for AFK-replaced human seats" in:
        val replaced = human(humanId).replaceWithABot()
        val lobby = (replaced - bot(botId) - bot(otherBotId))
          .asLobby(LobbyStatus.IN_GAME)
          .copy(configuration = GameConfiguration(timer = 60))
        val (verticle, _, redis, _, _) = createHarness(lobby)

        verticle.handleTurnEvent(LobbyPlayer(defaultLobbyId, humanId).toJson).futureValue

        redis.commands shouldBe List(
          (
            Command.SET,
            List(
              ChannelsKeys.turnTimer(defaultLobbyId, humanId),
              "1",
              "EX",
              GameConfiguration.BOT_TURN_TIMEOUT_SECONDS.toString
            )
          )
        )

      "apply strike-decayed TTL for active human players" in:
        val lobby = (human(humanId).withStrikes(1) - bot(botId) - bot(otherBotId))
          .asLobby(LobbyStatus.IN_GAME)
          .copy(configuration = GameConfiguration(timer = 30))
        val (verticle, _, redis, _, _) = createHarness(lobby)

        verticle.handleTurnEvent(LobbyPlayer(defaultLobbyId, humanId).toJson).futureValue

        redis.commands shouldBe List(
          (
            Command.SET,
            List(ChannelsKeys.turnTimer(defaultLobbyId, humanId), "1", "EX", "16")
          )
        )

      "ignore events for paused lobbies, unknown players, or malformed payloads" in:
        val pausedLobby = (human(humanId) - bot(botId) - bot(otherBotId))
          .asLobby(LobbyStatus.PAUSED)
        val (pausedVerticle, _, pausedRedis, _, _) = createHarness(pausedLobby)
        pausedVerticle.handleTurnEvent(LobbyPlayer(defaultLobbyId, botId).toJson).futureValue
        pausedRedis.commands shouldBe empty

        val activeLobby = (human(humanId) - bot(botId) - bot(otherBotId))
          .asLobby(LobbyStatus.IN_GAME)
        val (activeVerticle, _, activeRedis, _, _) = createHarness(activeLobby)
        activeVerticle.handleTurnEvent(LobbyPlayer(defaultLobbyId, PlayerId(99)).toJson).futureValue
        activeVerticle.handleTurnEvent("{not-json").futureValue
        activeRedis.commands shouldBe empty

    "handling turn expiration" should:
      "force a fallback action for bots without incrementing strikes or emitting offline events" in:
        val lobby = (human(humanId) - bot(botId) - bot(otherBotId))
          .asLobby(LobbyStatus.IN_GAME)
        val (verticle, pubSub, _, inbound, lobbyPort) = createHarness(lobby)

        verticle.handleTurnTimeout(defaultLobbyId, botId).futureValue

        inbound.forcedFallbacks shouldBe List((defaultLobbyId, botId))
        pubSub.published shouldBe empty
        lobbyPort
          .currentLobby(defaultLobbyId)
          .value
          .players
          .find(_.id == botId)
          .value
          .strikes shouldBe 0

      "increment strikes and force fallback for humans below maxStrikes" in:
        val lobby = (human(humanId) - bot(botId) - bot(otherBotId))
          .asLobby(LobbyStatus.IN_GAME)
          .copy(configuration = GameConfiguration(maxStrikes = 2))
        val (verticle, pubSub, _, inbound, lobbyPort) = createHarness(lobby)

        verticle.handleTurnTimeout(defaultLobbyId, humanId).futureValue

        inbound.forcedFallbacks shouldBe List((defaultLobbyId, humanId))
        pubSub.published shouldBe empty
        val updatedHuman =
          lobbyPort.currentLobby(defaultLobbyId).value.players.find(_.id == humanId).value
        updatedHuman.strikes shouldBe 1
        updatedHuman.isOnline shouldBe true

      "mark humans offline and emit SystemEvent.offline when maxStrikes is reached" in:
        val lobby = (human(humanId).withStrikes(1) - bot(botId) - bot(otherBotId))
          .asLobby(LobbyStatus.IN_GAME)
          .copy(configuration = GameConfiguration(maxStrikes = 2))
        val (verticle, pubSub, _, inbound, lobbyPort) = createHarness(lobby)

        verticle.handleTurnTimeout(defaultLobbyId, humanId).futureValue

        inbound.forcedFallbacks shouldBe empty
        pubSub.published shouldBe List(
          (ChannelsKeys.pubSubLobbyChannel(defaultLobbyId), SystemEvent.offline(humanId).toJson)
        )
        val updatedHuman =
          lobbyPort.currentLobby(defaultLobbyId).value.players.find(_.id == humanId).value
        updatedHuman.strikes shouldBe 2
        updatedHuman.isOnline shouldBe false

      "ignore expirations for unknown players or non-active lobbies" in:
        val lobby = (human(humanId) - bot(botId) - bot(otherBotId))
          .asLobby(LobbyStatus.IN_GAME)
        val (verticle, pubSub, _, inbound, _) = createHarness(lobby)

        verticle.handleTurnTimeout(defaultLobbyId, PlayerId(99)).futureValue
        inbound.forcedFallbacks shouldBe empty
        pubSub.published shouldBe empty
