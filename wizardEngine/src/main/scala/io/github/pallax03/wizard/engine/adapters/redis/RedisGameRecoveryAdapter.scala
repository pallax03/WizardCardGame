package io.github.pallax03.wizard.engine.adapters.redis

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

import io.vertx.redis.client.{Command, Redis, Request}

import io.github.pallax03.wizard.codecs.engine.model.core.state.GameStateCodecs.given
import io.github.pallax03.wizard.codecs.syntax.CodecSyntax._
import io.github.pallax03.wizard.engine.lobby.{LobbyId, LobbyStatus}
import io.github.pallax03.wizard.engine.model.core.GameException
import io.github.pallax03.wizard.engine.model.core.state.ServerGameState
import io.github.pallax03.wizard.engine.model.events.LifecycleEvent
import io.github.pallax03.wizard.engine.ports.{GameRecoveryPort, LobbyStatePort, OutboundPort}
import io.github.pallax03.wizard.util.FutureSyntax._
import io.github.pallax03.wizard.util.{ChannelsKeys, WizardLogger}

class RedisGameRecoveryAdapter(
    private val redisClient: Redis,
    private val lobbyStatePort: LobbyStatePort,
    private val outboundPort: OutboundPort
) extends GameRecoveryPort:

  override def attemptRecovery(lobbyId: LobbyId, exception: GameException): Future[Boolean] =
    val gameKey = ChannelsKeys.game(lobbyId)
    val checkpointKey = ChannelsKeys.gameCheckpoint(lobbyId)

    WizardLogger.warn(s"Attempting Checkpoint-Based Escalation Recovery for lobby $lobbyId")

    redisClient
      .send(Request.cmd(Command.GET).arg(checkpointKey))
      .asScala
      .flatMap:
        case null =>
          WizardLogger.error(
            s"Recovery FAILED: No checkpoint found for lobby $lobbyId"
          )
          abortGame(lobbyId, gameKey, checkpointKey, exception)
        case response =>
          val checkpointJson = response.toString
          checkpointJson.decodeAs[ServerGameState] match
            case Right(_) =>
              WizardLogger.info(
                s"Recovery SUCCESS: Checkpoint parsed. Restoring state for lobby $lobbyId"
              )
              restoreCheckpoint(lobbyId, gameKey, checkpointJson)
            case Left(err) =>
              WizardLogger.error(
                s"Recovery FAILED: Checkpoint is corrupted for lobby $lobbyId ($err)"
              )
              abortGame(lobbyId, gameKey, checkpointKey, exception)
      .recoverWith:
        case err =>
          WizardLogger.error(s"Recovery FAILED with exception.", err)
          abortGame(lobbyId, gameKey, checkpointKey, exception)

  private def restoreCheckpoint(
      lobbyId: LobbyId,
      gameKey: String,
      checkpointJson: String
  ): Future[Boolean] =
    val restoreReq = Request.cmd(Command.SET).arg(gameKey).arg(checkpointJson)
    redisClient
      .send(restoreReq)
      .asScala
      .flatMap: _ =>
        outboundPort.publish(
          lobbyId,
          LifecycleEvent.StateRecovered(round = -1)
        )
        Future.successful(true)

  private def abortGame(
      lobbyId: LobbyId,
      gameKey: String,
      checkpointKey: String,
      exception: GameException
  ): Future[Boolean] =
    WizardLogger.error(
      s"Recovery (Game Abort) executing for lobby $lobbyId"
    )
    val delReq = Request.cmd(Command.DEL).arg(gameKey).arg(checkpointKey)
    redisClient.send(delReq).asScala.flatMap { _ =>
      lobbyStatePort.getLobby(lobbyId).flatMap {
        case Some(lobby) =>
          val updatedLobby = lobby.copy(status = LobbyStatus.WAITING)
          lobbyStatePort.saveLobby(updatedLobby).flatMap { _ =>
            outboundPort.publish(lobbyId, LifecycleEvent.GameAborted(exception.getMessage))
            Future.successful(false)
          }
        case None =>
          outboundPort.publish(lobbyId, LifecycleEvent.GameAborted(exception.getMessage))
          Future.successful(false)
      }
    }
