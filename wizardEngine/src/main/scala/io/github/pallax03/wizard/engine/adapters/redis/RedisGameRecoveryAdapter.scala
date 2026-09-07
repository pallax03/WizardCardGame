package io.github.pallax03.wizard.engine.adapters.redis

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

import io.vertx.redis.client.{Command, Redis, Request}

import io.github.pallax03.wizard.codecs.engine.model.core.state.GameStateCodecs.given
import io.github.pallax03.wizard.codecs.syntax.CodecSyntax.*
import io.github.pallax03.wizard.engine.lobby.{LobbyId, LobbyStatus}
import io.github.pallax03.wizard.engine.model.core.state.{
  GameState,
  ServerCoreState,
  ServerGameState
}
import io.github.pallax03.wizard.engine.model.core.{GameEngine, GameException}
import io.github.pallax03.wizard.engine.model.events.LifecycleEvent
import io.github.pallax03.wizard.engine.ports.{
  GameRecoveryPort,
  LobbyStatePort,
  OutboundPort,
  PubSubPort
}
import io.github.pallax03.wizard.util.ChannelsKeys
import io.github.pallax03.wizard.util.FutureSyntax.*

class RedisGameRecoveryAdapter(
    private val redisClient: Redis,
    private val lobbyStatePort: LobbyStatePort,
    private val outboundPort: OutboundPort,
    private val pubSubPort: PubSubPort
) extends GameRecoveryPort:

  override def attemptRecovery(lobbyId: LobbyId, exception: GameException): Future[Boolean] =
    pubSubPort.publish(
      ChannelsKeys.LOGS_CHANNEL,
      s"ERROR:Attempting game recovery for lobby $lobbyId due to GameException: ${exception.getMessage}"
    )

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
              pubSubPort.publish(
                ChannelsKeys.LOGS_CHANNEL,
                s"WARN:Checkpoint contains ended game. Aborting."
              )
              abortGame(lobbyId, exception)

            case Left(_) =>
              pubSubPort.publish(ChannelsKeys.LOGS_CHANNEL, s"WARN:Checkpoint corrupted. Aborting.")
              abortGame(lobbyId, exception)

        case None =>
          pubSubPort.publish(
            ChannelsKeys.LOGS_CHANNEL,
            s"WARN:Checkpoint missing for lobby $lobbyId. Aborting game."
          )
          abortGame(lobbyId, exception)

    processCheckpoint.recoverWith { case e =>
      pubSubPort.publish(
        ChannelsKeys.LOGS_CHANNEL,
        s"ERROR:Failed during recovery process for lobby $lobbyId: ${e.getMessage}"
      )
      abortGame(lobbyId, exception)
    }

  private def recoverCheckpointWithNewRound(
      lobbyId: LobbyId,
      core: ServerCoreState
  ): Future[Boolean] =
    pubSubPort.publish(ChannelsKeys.LOGS_CHANNEL, s"INFO:Restoring checkpoint for lobby $lobbyId")
    redisClient
      .send(
        Request
          .cmd(Command.DEL)
          .arg(ChannelsKeys.gameCheckpoint(lobbyId))
      )
      .asScala
      .map: _ =>
        pubSubPort.publish(
          ChannelsKeys.LOGS_CHANNEL,
          s"INFO:Removing checkpoint for lobby:$lobbyId"
        )
        true
      .flatMap: _ =>
        val engine = GameEngine.recoverRound(core)
        redisClient
          .send(
            Request
              .cmd(Command.SET)
              .arg(ChannelsKeys.game(lobbyId))
              .arg(engine.state.toJson)
          )
          .asScala
          .map: _ =>
            outboundPort.publish(lobbyId, LifecycleEvent.StateRecovered() +: engine.events*)
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
      .flatMap: _ =>
        lobbyStatePort
          .getLobby(lobbyId)
          .flatMap:
            case Some(lobby) => lobbyStatePort.saveLobby(lobby.copy(status = LobbyStatus.WAITING))
            case None        => Future.unit
      .map: _ =>
        outboundPort.publish(lobbyId, LifecycleEvent.GameAborted(exception.getMessage))
        false
