package it.unibo.pps.wizard.application.bot

import io.vertx.core.AbstractVerticle
import it.unibo.pps.wizard.application.bot.strategy.BotStrategy
import it.unibo.pps.wizard.codecs.engine.model.WizardEventsCodecs.given
import it.unibo.pps.wizard.codecs.syntax.CodecSyntax._
import it.unibo.pps.wizard.engine.lobby.Lobby
import it.unibo.pps.wizard.engine.lobby.LobbyId
import it.unibo.pps.wizard.engine.lobby.LobbyStatus.IN_GAME
import it.unibo.pps.wizard.engine.model.basic.PlayerId
import it.unibo.pps.wizard.engine.model.core.state.GameState
import it.unibo.pps.wizard.engine.model.core.state.PlayerCoreState
import it.unibo.pps.wizard.engine.model.events.FailureEvent
import it.unibo.pps.wizard.engine.model.events.InvitationEvent
import it.unibo.pps.wizard.engine.model.events.LifecycleEvent
import it.unibo.pps.wizard.engine.model.events.WizardEvent
import it.unibo.pps.wizard.engine.ports.AIPort
import it.unibo.pps.wizard.engine.ports.InboundPort
import it.unibo.pps.wizard.engine.ports.LobbyStatePort
import it.unibo.pps.wizard.engine.ports.PubSubPort
import it.unibo.pps.wizard.engine.ports.Subscription
import it.unibo.pps.wizard.util.ChannelsKeys
import org.slf4j.LoggerFactory

import scala.collection.concurrent.TrieMap
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Failure
import scala.util.Success

class BotManagerVerticle(
    pubSubPort: PubSubPort,
    prologPort: AIPort,
    lobbyStatePort: LobbyStatePort,
    gameInboundPort: InboundPort
) extends AbstractVerticle:

  private val logger = LoggerFactory.getLogger(classOf[BotManagerVerticle])
  private val activeSubscriptions = TrieMap.empty[(LobbyId, PlayerId), (Subscription, Subscription)]

  private val podId = java.util.UUID.randomUUID().toString

  override def start(): Unit =
    logger.warn("BotManagerVerticle started. Subscribing to " + ChannelsKeys.SPAWN_BOT_CHANNEL)
    pubSubPort.subscribe(ChannelsKeys.SPAWN_BOT_CHANNEL, processSpawnEvent)

    logger.warn("Subscribing to old lobbies")
    lobbyStatePort.getAllLobbies.onComplete:
      case Success(lobbies) =>
        lobbies.filter(_.status == IN_GAME).foreach(spawnBotsForLobby)
      case Failure(_) => ()

  private def processSpawnEvent(lobbyIdStr: String): Unit =
    logger.warn(s"Received spawn request for lobby: $lobbyIdStr")
    lobbyStatePort
      .getLobby(LobbyId(lobbyIdStr))
      .onComplete:
        case Success(Some(lobby)) => spawnBotsForLobby(lobby)
        case _                    => logger.error(s"Failed to retrieve lobby $lobbyIdStr for spawn")

  private def spawnBotsForLobby(lobby: Lobby): Unit = {
    lobbyStatePort
      .tryAcquireBotLock(lobby.uuid, podId)
      .onComplete:
        case Success(true) =>
          logger.warn(s"Lock acquired for lobby ${lobby.uuid}. Spawning bots...")
          lobby.players
            .filter(_.difficulty.isDefined)
            .foreach: bot =>
              val strategy = BotStrategy(bot.difficulty.get, prologPort)

              if !activeSubscriptions.contains((lobby.uuid, bot.id)) then
                val handler = handleGameEvents(lobby.uuid, bot.id, strategy)
                for
                  playerSub <- pubSubPort.subscribeToPlayer(lobby.uuid, bot.id, handler)
                  lobbySub <- pubSubPort.subscribeToLobby(lobby.uuid, handler)
                yield activeSubscriptions.put((lobby.uuid, bot.id), (playerSub, lobbySub))
              syncStateAndPlay(lobby.uuid, bot.id, strategy)
        case _ =>
          logger.info(s"Lobby ${lobby.uuid} bots are managed by another pod.")
  }

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
            logger.warn(
              s"Bot $botId deduced pending action from GameState: ${inv.getClass.getSimpleName}"
            )
            executeInvitationStrategy(lobbyId, botId, strategy, inv)
        case Failure(e) =>
          logger.error(s"Bot $botId failed to sync GameState", e)

  private def handleGameEvents(lobbyId: LobbyId, playerId: PlayerId, strategy: BotStrategy)(
      rawJson: String
  ): Unit =
    rawJson.decodeAs[WizardEvent] match
      case Right(invitation: InvitationEvent) if playerId == invitation.playerId =>
        executeInvitationStrategy(lobbyId, playerId, strategy, invitation)
      case Right(failure: FailureEvent) if playerId == failure.destinationId =>
        strategy
          .resolveFailedEvents(lobbyId, failure)
          .onComplete:
            case Success(action) =>
              gameInboundPort.submitAction(lobbyId, action)
            case Failure(e) => logger.error(s"Bot $playerId fallback failed", e)
      case Right(LifecycleEvent.GameEnded(_, _)) =>
        activeSubscriptions.remove((lobbyId, playerId)).foreach: (playerSub, lobbySub) =>
          playerSub.cancel()
          lobbySub.cancel()
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
      .onComplete:
        case Success(action) =>
          gameInboundPort.submitAction(lobbyId, action)
        case Failure(e) =>
          logger.error(s"Bot $playerId strategy failed", e)
