"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { addBotAction, leaveLobbyAction } from "@/features/lobby/actions/manage-actions";
import { useLobbySession } from "@/features/lobby-session";
import { lobbyI18n } from "@/i18n/lobby";


import { LobbyHeader } from "@/features/lobby/components/LobbyHeader";
import { PlayerList } from "@/features/lobby/components/PlayerList";
import { LobbyActions } from "@/features/lobby/components/LobbyActions";
import { LobbyViewProps } from "../types";
import { getErrorMessage } from "@/ui/i18n/errors";

export function LobbyView({ maxPlayers = 6 }: LobbyViewProps) {
  const router = useRouter();
  const { lobby, playerId, connectionState, refreshLobby, connectedPlayerIds, error: sessionError } = useLobbySession();

  const [isAddingBot, setIsAddingBot] = useState<boolean>(false);
  const [removingBotId, setRemovingBotId] = useState<string | number | null>(null);
  const [activeBotSlot, setActiveBotSlot] = useState<number | null>(null);
  const [isLeaving, setIsLeaving] = useState<boolean>(false);
  const [actionError, setActionError] = useState<string | null>(null);

  const handleLeaveLobby = async () => {
    setActionError(null);
    if (lobby?.lobbyId && playerId !== null) {
      setIsLeaving(true);
      const result = await leaveLobbyAction(lobby.lobbyId, playerId);
      if (result?.error) {
        setActionError(getErrorMessage(result.error));
        setIsLeaving(false);
        return;
      }
    }
    localStorage.removeItem("wizard_lobbyId");
    localStorage.removeItem("wizard_playerId");
    router.push("/");
  };

  const handleStartGame = () => {
    if (lobby?.lobbyId) {
      router.push(`/game/${lobby.lobbyId}`);
    }
  };

  const handleAddBot = async (difficulty: string) => {
    if (!lobby?.lobbyId) return;
    if (difficulty !== "Dumb" && difficulty !== "Prolog") return;
    setActionError(null);
    setIsAddingBot(true);
    const result = await addBotAction(lobby.lobbyId, difficulty);
    
    if (result.error) {
      setActionError(getErrorMessage(result.error));
    } else {
      await refreshLobby();
    }
    setIsAddingBot(false);
  };

  const handleRemoveBot = async (botId: number) => {
    if (!lobby?.lobbyId) return;
    setActionError(null);
    setRemovingBotId(botId);
    const result = await leaveLobbyAction(lobby.lobbyId, botId);
    
    if (result?.error) {
      setActionError(getErrorMessage(result.error));
    } else {
      await refreshLobby();
    }
    setRemovingBotId(null);
  };

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
        {/* Traduzione del codice d'errore proveniente dalla sessione */}
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

      {/* Box opzionale per mostrare errori temporanei delle azioni (es. rimozione bot fallita) */}
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