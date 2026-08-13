package it.unibo.pps.wizard.application.bot

import io.vertx.core.AbstractVerticle
import it.unibo.pps.wizard.application.bot.strategy.BotStrategy
import it.unibo.pps.wizard.engine.ports.{AIPort, InboundPort, LobbyStatePort, PubSubPort}
import it.unibo.pps.wizard.codecs.syntax.CodecSyntax.*
import it.unibo.pps.wizard.codecs.engine.model.WizardEventsCodecs.given
import it.unibo.pps.wizard.engine.lobby.LobbyId
import it.unibo.pps.wizard.engine.model.basic.PlayerId
import it.unibo.pps.wizard.engine.model.events.{FailureEvent, InvitationEvent, WizardEvent}
import it.unibo.pps.wizard.util.ChannelsKeys

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}

class BotManagerVerticle(pubSubPort: PubSubPort, prologPort: AIPort, lobbyStatePort: LobbyStatePort, gameInboundPort: InboundPort) extends AbstractVerticle:

  override def start(): Unit =
    pubSubPort.subscribe("bots:global:spawn", handleSpawn)

  private def handleSpawn(lobbyIdStr: String): Unit =
    val lobbyId = LobbyId(lobbyIdStr)
    
    lobbyStatePort.getLobby(lobbyId).onComplete:
      case Success(Some(lobby)) =>
        lobby.players.filter(_.difficulty.isDefined).foreach: bot =>
          val channel = ChannelsKeys.pubSubLobbyPlayerChannel(lobbyId, bot.id)
          pubSubPort.subscribe(channel, handleGameEvents(lobbyId, bot.id, BotStrategy(bot.difficulty.get, prologPort)))
      case _ => ()

  private def handleGameEvents(lobbyId: LobbyId, playerId: PlayerId, strategy: BotStrategy)(rawJson: String): Unit =
    rawJson.decodeAs[WizardEvent] match
      case Right(invitation: InvitationEvent) if playerId == invitation.playerId =>
        strategy.resolveInvitationEvents(lobbyId, invitation).onComplete:
          case Success(action) => gameInboundPort.submitAction(lobbyId, action)
          case Failure(exception) => ()
      case Right(failure: FailureEvent) if playerId == failure.destinationId =>
        strategy.resolveFailedEvents(lobbyId, failure).onComplete:
          case Success(action) => gameInboundPort.submitAction(lobbyId, action)
          case Failure(exception) => ()
      case Right(_) => ()
      case Left(_) => ()