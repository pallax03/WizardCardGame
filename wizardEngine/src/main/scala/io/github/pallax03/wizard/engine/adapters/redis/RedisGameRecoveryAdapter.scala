package io.github.pallax03.wizard.engine.adapters.redis

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

import io.vertx.redis.client.{Command, Redis, Request}

import io.github.pallax03.wizard.codecs.engine.model.core.state.GameStateCodecs.given
import io.github.pallax03.wizard.codecs.syntax.CodecSyntax.*
import io.github.pallax03.wizard.engine.lobby.LobbyId
import io.github.pallax03.wizard.engine.model.core.state.{
  GameState,
  ServerCoreState,
  ServerGameState
}
import io.github.pallax03.wizard.engine.model.core.{GameEngine, GameException}
import io.github.pallax03.wizard.engine.model.events.LifecycleEvent
import io.github.pallax03.wizard.engine.ports.{GameRecoveryPort, OutboundPort}
import io.github.pallax03.wizard.util.FutureSyntax.*
import io.github.pallax03.wizard.util.{ChannelsKeys, LogContext, RedisUtil, WizardLogger}

class RedisGameRecoveryAdapter(
    private val redisClient: Redis,
    private val outboundPort: OutboundPort
) extends GameRecoveryPort:

  override def attemptRecovery(lobbyId: LobbyId, exception: GameException): Future[Boolean] =
    given LogContext = LogContext(lobbyId)
    WizardLogger.error(s"Attempting game recovery due to GameException: ${exception.getMessage}")

    val processCheckpoint = redisClient
      .send(Request.cmd(Command.GET).arg(ChannelsKeys.gameCheckpoint(lobbyId)))
      .asScala
      .map(Option(_).map(_.toString))
      .flatMap:
        case Some(json) =>
          json.decodeAs[ServerGameState] match
            case Right(GameState.ChoosingTrump(core)) =>
              recoverCheckpointWithNewRound(lobbyId, core)
            case Right(GameState.Bidding(core, _, _)) =>
              recoverCheckpointWithNewRound(lobbyId, core)
            case Right(GameState.Playing(core, _, _, _, _)) =>
              recoverCheckpointWithNewRound(lobbyId, core)

            case Right(GameState.Ended(_, _)) =>
              WizardLogger.warn("Checkpoint contains ended game. Aborting.")
              abortGame(lobbyId, exception)

            case Left(_) =>
              WizardLogger.warn("Checkpoint corrupted. Aborting.")
              abortGame(lobbyId, exception)

        case None =>
          WizardLogger.warn("Checkpoint missing. Aborting game.")
          abortGame(lobbyId, exception)

    processCheckpoint.recoverWith { case e =>
      WizardLogger.error(s"Failed during recovery process: ${e.getMessage}")
      abortGame(lobbyId, exception)
    }

  private def recoverCheckpointWithNewRound(
      lobbyId: LobbyId,
      core: ServerCoreState
  ): Future[Boolean] =
    given LogContext = LogContext(lobbyId)
    WizardLogger.info("Restoring checkpoint")
    redisClient
      .send(
        Request
          .cmd(Command.DEL)
          .arg(ChannelsKeys.gameCheckpoint(lobbyId))
      )
      .asScala
      .map: _ =>
        WizardLogger.info("Removing checkpoint")
        true
      .flatMap: _ =>
        val engine = GameEngine.recoverRound(core)
        redisClient
          .send(
            RedisUtil.setWithDefaultTTL(ChannelsKeys.game(lobbyId), engine.state.toJson)
          )
          .asScala
          .map: _ =>
            outboundPort.publish(lobbyId, LifecycleEvent.StateRecovered +: engine.events*)
            true

  private def abortGame(
      lobbyId: LobbyId,
      exception: GameException
  ): Future[Boolean] =
    redisClient
      .send(
        Request
          .cmd(Command.DEL)
          .arg(ChannelsKeys.game(lobbyId))
          .arg(ChannelsKeys.gameCheckpoint(lobbyId))
      )
      .asScala
      .map: _ =>
        outboundPort.publish(lobbyId, LifecycleEvent.GameCancelled(Option(exception.getMessage)))
        false
