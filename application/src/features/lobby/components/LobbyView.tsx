"use client";

import { useRouter } from "next/navigation";
import { useLobby } from "../hooks/useLobby"; // O il percorso corretto al file
import { t } from "@/ui/i18n/core";
const lobbyI18n = t("lobby");

import { LobbyHeader } from "@/features/lobby/components/LobbyHeader";
import { PlayerList } from "@/features/lobby/components/PlayerList";
import { LobbyActions } from "@/features/lobby/components/LobbyActions";
import { LobbyViewProps } from "../types";
import { getErrorMessage } from "@/ui/i18n/errors";

export function LobbyView({ maxPlayers = 6 }: LobbyViewProps) {
  const router = useRouter();
  const {
    lobby,
    playerId,
    connectionState,
    connectedPlayerIds,
    sessionError,
    actionError,
    isAddingBot,
    removingBotId,
    activeBotSlot,
    isLeaving,
    setActiveBotSlot,
    handleLeaveLobby,
    handleStartGame,
    handleAddBot,
    handleRemoveBot,
  } = useLobby();

  if (!lobby && connectionState === "connecting") {
    return (
      <div className="flex items-center justify-center min-h-100">
        <p suppressHydrationWarning className="text-slate-400 animate-pulse font-medium">{lobbyI18n.loading}</p>
      </div>
    );
  }

  if (sessionError) {
    localStorage.removeItem("wizard_lobbyId");
    localStorage.removeItem("wizard_playerId");
    return (
      <div className="flex flex-col items-center justify-center min-h-100 gap-4">
        <p className="text-red-400 font-medium">{getErrorMessage(sessionError.message)}</p>
        <button onClick={() => router.push("/")} className="px-4 py-2 border rounded-md text-slate-200">
          {lobbyI18n.backToHome}
        </button>
      </div>
    );
  }

  const players = lobby?.players || [];

  return (
    <div className="w-full max-w-4xl space-y-6">
      {connectionState === "reconnecting" && (
        <div className="bg-amber-500/10 border border-amber-500/20 text-amber-400 px-4 py-2 rounded-md text-xs text-center animate-pulse">
          {lobbyI18n.reconnecting}
        </div>
      )}

      {actionError && (
        <div className="bg-red-500/10 border border-red-500/20 text-red-400 px-4 py-2 rounded-md text-xs text-center">
          {actionError}
        </div>
      )}

      <LobbyHeader lobbyCode={lobby?.lobbyId || ""} />
      
      <PlayerList
        players={players}
        maxPlayers={maxPlayers}
        currentUserId={playerId}
        connectedPlayerIds={connectedPlayerIds}
        activeBotSlot={activeBotSlot}
        isAddingBot={isAddingBot}
        removingBotId={removingBotId}
        onSelectBotSlot={setActiveBotSlot}
        onAddBot={handleAddBot}
        onRemoveBot={handleRemoveBot}
      />
      
      <LobbyActions
        isLeaving={isLeaving}
        onLeave={handleLeaveLobby}
        onStart={handleStartGame}
      />
    </div>
  );
}