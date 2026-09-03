package io.github.pallax03.wizard.application.bot

import scala.collection.concurrent.TrieMap
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success}

import cats.syntax.all.*

import io.vertx.core.AbstractVerticle

import io.github.pallax03.wizard.application.bot.strategy.BotStrategy
import io.github.pallax03.wizard.codecs.engine.model.WizardEventsCodecs.given
import io.github.pallax03.wizard.codecs.syntax.CodecSyntax.*
import io.github.pallax03.wizard.engine.lobby.LobbyStatus.IN_GAME
import io.github.pallax03.wizard.engine.lobby.{Lobby, LobbyId}
import io.github.pallax03.wizard.engine.model.basic.PlayerId
import io.github.pallax03.wizard.engine.model.core.state.{GameState, PlayerCoreState}
import io.github.pallax03.wizard.engine.model.events.*
import io.github.pallax03.wizard.engine.ports.*
import io.github.pallax03.wizard.util.ChannelsKeys

class BotManagerVerticle(
    pubSubPort: PubSubPort,
    prologPort: AIPort,
    lobbyStatePort: LobbyStatePort,
    gameInboundPort: InboundPort
) extends AbstractVerticle:

  private val activeSubscriptions = TrieMap.empty[(LobbyId, PlayerId), Subscription]

  private val podId = java.util.UUID.randomUUID().toString

  override def start(): Unit =
    pubSubPort.subscribe(ChannelsKeys.SPAWN_BOT_CHANNEL, processSpawnEvent)
    lobbyStatePort.getAllLobbies.onComplete:
      case Success(lobbies) =>
        lobbies.filter(_.status == IN_GAME).foreach(spawnBotsForLobby)
      case Failure(_) => ()

  private def processSpawnEvent(lobbyIdStr: String): Unit =
    lobbyStatePort
      .getLobby(LobbyId(lobbyIdStr))
      .onComplete:
        case Success(Some(lobby)) => spawnBotsForLobby(lobby)
        case _                    => ()

  private def spawnBotsForLobby(lobby: Lobby): Unit =
    lobbyStatePort
      .tryAcquireBotLock(lobby.uuid, podId)
      .onComplete:
        case Success(true) =>
          lobby.players
            .filter(_.difficulty.isDefined)
            .foreach: bot =>
              val strategy = BotStrategy(bot.difficulty.get, prologPort)
              if !activeSubscriptions.contains((lobby.uuid, bot.id)) then
                val handler = handleGameEvents(lobby.uuid, bot.id, strategy)
                pubSubPort
                  .subscribePlayer(lobby.uuid, bot.id, handler)
                  .map: sub =>
                    activeSubscriptions.put((lobby.uuid, bot.id), sub)
              syncStateAndPlay(lobby.uuid, bot.id, strategy)
        case _ => ()

  private def syncStateAndPlay(lobbyId: LobbyId, botId: PlayerId, strategy: BotStrategy): Unit =
    gameInboundPort
      .getState(lobbyId, botId)
      .onComplete:
        case Success(state) =>
          val invitationOpt = state match
            case GameState.Bidding(core: PlayerCoreState, _, turn) if turn == botId =>
              Some(InvitationEvent.WaitingForBid(botId, core.round))
            case GameState.Playing(core: PlayerCoreState, _, _, turn, _) if turn == botId =>
              Some(InvitationEvent.WaitingForCard(botId, core.hand.toList))
            case GameState.ChoosingTrump(core: PlayerCoreState) if core.dealerId == botId =>
              Some(InvitationEvent.WaitingForTrump(botId))
            case _ => None

          invitationOpt.foreach: inv =>
            executeInvitationStrategy(lobbyId, botId, strategy, inv)
        case Failure(_) => ()

  private def handleGameEvents(lobbyId: LobbyId, playerId: PlayerId, strategy: BotStrategy)(
      rawJson: String
  ): Unit =
    rawJson.decodeAs[WizardEvent] match
      case Right(invitation: InvitationEvent) if playerId == invitation.playerId =>
        executeInvitationStrategy(lobbyId, playerId, strategy, invitation)
      case Right(LifecycleEvent.GameEnded(_, _)) =>
        activeSubscriptions
          .remove((lobbyId, playerId))
          .foreach: sub =>
            sub.cancel()
      case Right(_) => ()
      case Left(_)  => ()

  private def executeInvitationStrategy(
      lobbyId: LobbyId,
      playerId: PlayerId,
      strategy: BotStrategy,
      invitation: InvitationEvent
  ): Unit =
    strategy
      .resolveInvitationEvents(lobbyId, invitation)
      .flatMap: action =>
        gameInboundPort
          .submitAction(lobbyId, action)
          .flatMap:
            case Left(gameError) =>
              strategy
                .resolveFailedEvents(lobbyId, FailureEvent.ActionFailed(playerId, gameError))
                .flatMap: fallbackAction =>
                  gameInboundPort.submitAction(lobbyId, fallbackAction).void
            case Right(_) => Future.unit
      .onComplete:
        case Failure(_) => ()
        case _          => ()
