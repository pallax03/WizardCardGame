"use client";

import { useState, useEffect } from "react";
import { useRouter } from "next/navigation";
import { addBotAction, leaveLobbyAction, startGameAction } from "@/features/lobby/api";
import { useLobbySession } from "@/features/lobby-session";
import { getErrorMessage } from "@/ui/i18n/errors";

export function useLobby() {
  const router = useRouter();
  const { 
    lobby, 
    playerId, 
    connectionState, 
    refreshLobby, 
    connectedPlayerIds, 
    error: sessionError 
  } = useLobbySession();

  const [isAddingBot, setIsAddingBot] = useState<boolean>(false);
  const [removingBotId, setRemovingBotId] = useState<string | number | null>(null);
  const [activeBotSlot, setActiveBotSlot] = useState<number | null>(null);
  const [isLeaving, setIsLeaving] = useState<boolean>(false);
  const [isStarting, setIsStarting] = useState<boolean>(false);
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

  const handleStartGame = async () => {
    if (!lobby?.lobbyId) return;

    setActionError(null);
    setIsStarting(true);

    const result = await startGameAction(lobby.lobbyId);

    if (result.error) {
      setActionError(getErrorMessage(result.error));
      setIsStarting(false);
    }
  };

  const handleAddBot = async (difficulty: string) => {
    if (!lobby?.lobbyId) return;
    if (difficulty !== "Dumb" && difficulty !== "Prolog") return;
    setActionError(null);
    setIsAddingBot(true);
    const result = await addBotAction(lobby.lobbyId, difficulty as "Dumb" | "Prolog");
    
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

  useEffect(() => {
    if (lobby && playerId !== null && !lobby.players.some((p) => p.id === playerId)) {
      localStorage.removeItem("wizard_lobbyId");
      localStorage.removeItem("wizard_playerId");
      router.push("/");
      return;
    }
  }, [lobby, playerId, router]);

  return {
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
    isStarting,
    setActiveBotSlot,
    handleLeaveLobby,
    handleStartGame,
    handleAddBot,
    handleRemoveBot,
  };
}