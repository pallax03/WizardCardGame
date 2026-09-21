"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";
import { useLobby } from "../hooks/useLobby";
import { removeSavedLobby } from "@/features/lobby-session/storage";
import { ArrowLeft } from "lucide-react";
import { t } from "@/ui/i18n/core";
const lobbyI18n = t("lobby");

import { LobbyHeader } from "@/features/lobby/components/LobbyHeader";
import { PlayerList } from "@/features/lobby/components/PlayerList";
import { LobbyActions } from "@/features/lobby/components/LobbyActions";
import { PausedGameSummary } from "@/features/lobby/components/PausedGameSummary";
import { LobbyConfiguration } from "@/features/lobby/components/LobbyConfiguration";
import { DisconnectingBanner } from "@/features/lobby/components/DisconnectingBanner";
import { useLobbyGameSnapshot } from "../hooks/useLobbyGameSnapshot";
import { LobbyViewProps } from "../types";
import { getErrorMessage } from "@/ui/i18n/errors";

export function LobbyView({ maxPlayers = 6 }: LobbyViewProps) {
  const router = useRouter();
  const {
    lobby,
    lobbyId,
    playerId,
    connectionState,
    reconnect,
    connectedPlayerIds,
    sessionError,
    actionError,
    isAddingBot,
    removingBotId,
    activeBotSlot,
    isLeaving,
    isStarting,
    isDiscarding,
    isPausing,
    isSavingConfig,
    setActiveBotSlot,
    handleLeaveLobby,
    handleBackToHome,
    handleStartGame,
    handleDiscardPausedGame,
    handlePauseGame,
    handleUpdateConfiguration,
    handleAddBot,
    handleRemoveBot,
  } = useLobby();

  useEffect(() => {
    if (sessionError) removeSavedLobby(lobbyId);
  }, [sessionError, lobbyId]);

  const players = lobby?.players || [];
  const status = lobby?.status;
  const isPaused = status === "PAUSED";
  const isFinished = status === "FINISHED";
  const isWaiting = status === "WAITING" || status === undefined;
  const isDisconnecting = status === "DISCONNECTING";
  const canManagePlayers = isWaiting;
  const canEditConfig = isWaiting || isPaused;

  const snapshotEnabled = Boolean(lobby?.lobbyId) && (isPaused || isFinished || isWaiting);
  const {
    board: savedBoard,
    isLoading: isSnapshotLoading,
    loadFailed: isSnapshotFailed,
  } = useLobbyGameSnapshot(lobby?.lobbyId ?? null, playerId, snapshotEnabled, lobby?.status);
  const showSummary = isPaused || isFinished || savedBoard !== null;
  const showDiscard = isPaused || isFinished;

  if (!lobby && connectionState === "connecting") {
    return (
      <div className="flex items-center justify-center min-h-100">
        <p suppressHydrationWarning className="text-slate-400 animate-pulse font-medium">{lobbyI18n.loading}</p>
      </div>
    );
  }

  if (sessionError) {
    return (
      <div className="flex flex-col items-center justify-center min-h-100 gap-4">
        <p className="text-red-400 font-medium">{getErrorMessage(sessionError.message)}</p>
        <button onClick={() => router.push("/")} className="px-4 py-2 border rounded-md text-slate-200">
          {lobbyI18n.backToHome}
        </button>
      </div>
    );
  }

  return (
    <div className="w-full max-w-4xl space-y-6">
      <button
        type="button"
        onClick={handleBackToHome}
        className="flex items-center gap-1.5 text-xs font-medium text-zinc-400 hover:text-zinc-100 transition-colors"
      >
        <ArrowLeft className="w-3.5 h-3.5" /> {lobbyI18n.backToLobbies}
      </button>
      {connectionState === "reconnecting" && (
        <div className="bg-amber-500/10 border border-amber-500/20 text-amber-400 px-4 py-2 rounded-md text-xs text-center animate-pulse">
          {lobbyI18n.reconnecting}
        </div>
      )}
      {connectionState === "closed" && (
        <div className="bg-red-500/10 border border-red-500/30 text-red-300 px-4 py-2 rounded-md text-xs text-center flex items-center justify-center gap-3">
          <span>{lobbyI18n.reconnectClosed}</span>
          <button
            type="button"
            onClick={reconnect}
            className="px-3 py-1 rounded-md bg-red-500/20 border border-red-500/40 font-semibold hover:bg-red-500/30 transition-colors"
          >
            {lobbyI18n.retryConnection}
          </button>
        </div>
      )}

      {isPaused && (
        <div className="bg-sky-500/10 border border-sky-500/30 text-sky-300 px-4 py-2 rounded-md text-sm text-center font-semibold">
          ⏸️ {lobbyI18n.pausedNotice}
        </div>
      )}

      {isDisconnecting && (
        <DisconnectingBanner timerSeconds={lobby?.configuration?.timer ?? 30} />
      )}

      {isFinished && (
        <div className="bg-amber-500/10 border border-amber-500/30 text-amber-300 px-4 py-2 rounded-md text-sm text-center font-semibold">
          🏆 {lobbyI18n.finishedNotice}
        </div>
      )}

      {actionError && (
        <div className="bg-red-500/10 border border-red-500/20 text-red-400 px-4 py-2 rounded-md text-xs text-center">
          {actionError}
        </div>
      )}

      <LobbyHeader lobbyCode={lobby?.lobbyId || ""} />

      {showSummary && (
        <PausedGameSummary
          board={savedBoard}
          isLoading={isSnapshotLoading && !isWaiting}
          loadFailed={isSnapshotFailed}
          playerId={playerId}
          players={players}
        />
      )}
      
      <PlayerList
        players={players}
        maxPlayers={maxPlayers}
        currentUserId={playerId}
        connectedPlayerIds={connectedPlayerIds}
        activeBotSlot={activeBotSlot}
        isAddingBot={isAddingBot}
        removingBotId={removingBotId}
        canManagePlayers={canManagePlayers}
        onSelectBotSlot={setActiveBotSlot}
        onAddBot={handleAddBot}
        onRemoveBot={handleRemoveBot}
      />

      {lobby?.configuration && (
        <LobbyConfiguration
          timer={lobby.configuration.timer}
          maxStrikes={lobby.configuration.maxStrikes}
          canEdit={canEditConfig}
          isSaving={isSavingConfig}
          onSave={handleUpdateConfiguration}
        />
      )}
      
      <LobbyActions
        isLeaving={isLeaving}
        isStarting={isStarting}
        onLeave={handleLeaveLobby}
        onStart={handleStartGame}
        isResuming={isPaused}
        isDiscarding={isDiscarding}
        onDiscard={showDiscard ? handleDiscardPausedGame : undefined}
        disableStart={isFinished}
        discardMode={isFinished ? "finished" : "paused"}
        status={status}
        isPausing={isPausing}
        onPause={isDisconnecting ? handlePauseGame : undefined}
        onExitHome={handleBackToHome}
      />
    </div>
  );
}
