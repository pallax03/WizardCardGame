package it.unibo.pps.wizard.application.bot

import io.vertx.core.AbstractVerticle
import it.unibo.pps.wizard.application.bot.strategy.BotStrategy
import it.unibo.pps.wizard.engine.ports.{AIPort, InboundPort, LobbyStatePort, PubSubPort}
import it.unibo.pps.wizard.codecs.syntax.CodecSyntax.*
import it.unibo.pps.wizard.codecs.engine.model.WizardEventsCodecs.given
import it.unibo.pps.wizard.engine.lobby.LobbyId
import it.unibo.pps.wizard.engine.model.basic.PlayerId
import it.unibo.pps.wizard.engine.model.core.GameState
import it.unibo.pps.wizard.engine.model.events.{FailureEvent, InvitationEvent, WizardEvent}
import it.unibo.pps.wizard.util.ChannelsKeys
import org.slf4j.LoggerFactory

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}

class BotManagerVerticle(pubSubPort: PubSubPort, prologPort: AIPort, lobbyStatePort: LobbyStatePort, gameInboundPort: InboundPort) extends AbstractVerticle:

  private val logger = LoggerFactory.getLogger(classOf[BotManagerVerticle])
  private var activeSubscriptions = Set.empty[(LobbyId, PlayerId)]

  override def start(): Unit =
    logger.warn("BotManagerVerticle started. Subscribing to "+ ChannelsKeys.SPAWN_BOT_CHANNEL)
    pubSubPort.subscribe(ChannelsKeys.SPAWN_BOT_CHANNEL, handleSpawn)

  private def handleSpawn(lobbyIdStr: String): Unit =
    logger.warn(s"Received spawn request for lobby: $lobbyIdStr")
    val lobbyId = LobbyId(lobbyIdStr)
    
    lobbyStatePort.getLobby(lobbyId).onComplete:
      case Success(Some(lobby)) =>
        lobby.players.filter(_.difficulty.isDefined).foreach: bot =>
          val strategy = BotStrategy(bot.difficulty.get, prologPort)
          
          if !activeSubscriptions.contains((lobbyId, bot.id)) then
            activeSubscriptions += (lobbyId, bot.id)
            val channel = ChannelsKeys.pubSubLobbyChannel(lobbyId)
            val privateChannel = ChannelsKeys.pubSubLobbyPlayerChannel(lobbyId, bot.id)
            logger.warn(s"Subscribing bot ${bot.id} to channel: $channel")
            pubSubPort.subscribe(privateChannel, handleGameEvents(lobbyId, bot.id, strategy))
            pubSubPort.subscribe(channel, handleGameEvents(lobbyId, bot.id, strategy))
          syncStateAndPlay(lobbyId, bot.id, strategy)
          
      case _ => logger.error(s"Failed to retrieve lobby $lobbyIdStr for spawn")

  private def syncStateAndPlay(lobbyId: LobbyId, botId: PlayerId, strategy: BotStrategy): Unit =
    gameInboundPort.getState(lobbyId, botId).onComplete:
      case Success(state) =>
        val invitationOpt = state match
          case GameState.Bidding(core, _, turn) if turn == botId =>
            Some(InvitationEvent.WaitingForBid(botId, core.round))
          case GameState.Playing(core, _, _, turn, _) if turn == botId =>
            Some(InvitationEvent.WaitingForCard(botId, core.hands.getHand(botId).get.toList))
          case GameState.ChoosingTrump(core) if core.dealerId == botId =>
            Some(InvitationEvent.WaitingForTrump(botId))
          case _ => None

        invitationOpt.foreach: inv =>
          logger.warn(s"Bot $botId deduced pending action from GameState: ${inv.getClass.getSimpleName}")
          executeStrategy(lobbyId, botId, strategy, inv)
      case Failure(e) => 
        logger.error(s"Bot $botId failed to sync GameState", e)

  private def handleGameEvents(lobbyId: LobbyId, playerId: PlayerId, strategy: BotStrategy)(rawJson: String): Unit =
    rawJson.decodeAs[WizardEvent] match
      case Right(invitation: InvitationEvent) if playerId == invitation.playerId =>
        logger.warn(s"Bot $playerId received invitation: ${invitation.getClass.getSimpleName}")
        executeStrategy(lobbyId, playerId, strategy, invitation)
      case Right(failure: FailureEvent) if playerId == failure.destinationId =>
        logger.warn(s"Bot $playerId received failure event: $failure")
        strategy.resolveFailedEvents(lobbyId, failure).onComplete:
          case Success(action) => 
            logger.warn(s"Bot $playerId submitting fallback action: $action")
            gameInboundPort.submitAction(lobbyId, action)
          case Failure(e) => logger.error(s"Bot $playerId fallback failed", e)
      case Right(_) => // Ignore Other Events
      case Left(error) => logger.error("", error)

  private def executeStrategy(lobbyId: LobbyId, playerId: PlayerId, strategy: BotStrategy, invitation: InvitationEvent): Unit =
    strategy.resolveInvitationEvents(lobbyId, invitation).onComplete:
      case Success(action) => 
        logger.warn(s"Bot $playerId submitting action: $action")
        gameInboundPort.submitAction(lobbyId, action)
      case Failure(e) => 
        logger.error(s"Bot $playerId strategy failed", e)