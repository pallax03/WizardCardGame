package io.github.pallax03.wizard.engine.adapters.redis

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

import io.vertx.redis.client.{Command, Redis, Request}

import io.github.pallax03.wizard.codecs.engine.model.core.state.GameStateCodecs.given
import io.github.pallax03.wizard.codecs.syntax.CodecSyntax.*
import io.github.pallax03.wizard.engine.lobby.{LobbyId, LobbyStatus}
import io.github.pallax03.wizard.engine.model.core.GameException
import io.github.pallax03.wizard.engine.model.core.state.ServerGameState
import io.github.pallax03.wizard.engine.model.events.LifecycleEvent
import io.github.pallax03.wizard.engine.ports.{GameRecoveryPort, LobbyStatePort, OutboundPort}
import io.github.pallax03.wizard.util.ChannelsKeys
import io.github.pallax03.wizard.util.FutureSyntax.*

class RedisGameRecoveryAdapter(
    private val redisClient: Redis,
    private val lobbyStatePort: LobbyStatePort,
    private val outboundPort: OutboundPort
) extends GameRecoveryPort:

  override def attemptRecovery(lobbyId: LobbyId, exception: GameException): Future[Boolean] =
    val gameKey = ChannelsKeys.game(lobbyId)
    val checkpointKey = ChannelsKeys.gameCheckpoint(lobbyId)

    val processCheckpoint = redisClient
      .send(Request.cmd(Command.GET).arg(checkpointKey))
      .asScala
      .map(Option(_).map(_.toString))
      .flatMap:
        case Some(json) if json.decodeAs[ServerGameState].isRight =>
          restoreCheckpoint(lobbyId, gameKey, json)
        case _ => abortGame(lobbyId, gameKey, checkpointKey, exception)

    processCheckpoint.recoverWith { case _ =>
      abortGame(lobbyId, gameKey, checkpointKey, exception)
    }

  private def restoreCheckpoint(
      lobbyId: LobbyId,
      gameKey: String,
      checkpointJson: String
  ): Future[Boolean] =
    redisClient
      .send(Request.cmd(Command.SET).arg(gameKey).arg(checkpointJson))
      .asScala
      .map: _ =>
        outboundPort.publish(lobbyId, LifecycleEvent.StateRecovered())
        true

  private def abortGame(
      lobbyId: LobbyId,
      gameKey: String,
      checkpointKey: String,
      exception: GameException
  ): Future[Boolean] =
    redisClient
      .send(Request.cmd(Command.DEL).arg(gameKey).arg(checkpointKey))
      .asScala
      .flatMap: _ =>
        lobbyStatePort
          .getLobby(lobbyId)
          .flatMap:
            case Some(lobby) => lobbyStatePort.saveLobby(lobby.copy(status = LobbyStatus.WAITING))
            case None        => Future.unit
      .map: _ =>
        outboundPort.publish(lobbyId, LifecycleEvent.GameAborted(exception.getMessage))
        false
