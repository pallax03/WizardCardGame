"use client";

import { useState, useEffect } from "react";
import { useRouter } from "next/navigation";
import { addBotAction, discardPausedGameAction, leaveLobbyAction, pauseGameAction, startGameAction, updateConfigurationAction } from "@/features/lobby/api";
import { useLobbySession } from "@/features/lobby-session";
import { removeSavedLobby, saveLobby } from "@/features/lobby-session/storage";
import { getErrorMessage } from "@/ui/i18n/errors";

export function useLobby() {
  const router = useRouter();
  const {
    lobby,
    lobbyId,
    playerId,
    connectionState,
    refreshLobby,
    reconnect,
    awaitOpen,
    connectedPlayerIds,
    error: sessionError
  } = useLobbySession();

  const [isAddingBot, setIsAddingBot] = useState<boolean>(false);
  const [removingBotId, setRemovingBotId] = useState<number | null>(null);
  const [activeBotSlot, setActiveBotSlot] = useState<number | null>(null);
  const [isLeaving, setIsLeaving] = useState<boolean>(false);
  const [isStarting, setIsStarting] = useState<boolean>(false);
  const [isDiscarding, setIsDiscarding] = useState<boolean>(false);
  const [isPausing, setIsPausing] = useState<boolean>(false);
  const [isSavingConfig, setIsSavingConfig] = useState<boolean>(false);
  const [actionError, setActionError] = useState<string | null>(null);

  const handleLeaveLobby = async () => {
    setActionError(null);
    if (!lobby?.lobbyId || playerId === null || isLeaving) return;
    if (lobby.status !== "WAITING") {
      setActionError(getErrorMessage("GameInProgress"));
      return;
    }
    setIsLeaving(true);
    const result = await leaveLobbyAction(lobby.lobbyId, playerId);
    if (result?.error) {
      setActionError(getErrorMessage(result.error));
      setIsLeaving(false);
      return;
    }
    removeSavedLobby(lobby.lobbyId);
    router.push("/");
  };

  const handleBackToHome = () => {
    setActionError(null);
    if (lobby?.lobbyId && playerId !== null) {
      saveLobby(lobby.lobbyId, playerId);
    }
    router.push("/");
  };

  const handleStartGame = async () => {
    if (!lobby?.lobbyId || isStarting) return;

    setActionError(null);
    setIsStarting(true);

    if (connectionState !== "open") {
      reconnect();
      const connected = await awaitOpen(8000);
      if (!connected) {
        setActionError(getErrorMessage("RECONNECT_FAILED"));
        setIsStarting(false);
        return;
      }
    }

    const result = await startGameAction(lobby.lobbyId);

    if (result.error) {
      setActionError(getErrorMessage(result.error));
    } else {
      await refreshLobby();
    }
    setIsStarting(false);
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
    const result = await leaveLobbyAction(lobby.lobbyId, botId, false);
    
    if (result?.error) {
      setActionError(getErrorMessage(result.error));
    } else {
      await refreshLobby();
    }
    setRemovingBotId(null);
  };

  const handleDiscardPausedGame = async () => {
    if (!lobby?.lobbyId || isDiscarding) return;
    setActionError(null);
    setIsDiscarding(true);
    const result = await discardPausedGameAction(lobby.lobbyId);
    if (result.error) {
      setActionError(getErrorMessage(result.error));
    } else {
      await refreshLobby();
    }
    setIsDiscarding(false);
  };

  const handlePauseGame = async () => {
    if (!lobby?.lobbyId || isPausing) return;
    setActionError(null);
    setIsPausing(true);
    const result = await pauseGameAction(lobby.lobbyId);
    if (result.error) {
      setActionError(getErrorMessage(result.error));
    } else {
      await refreshLobby();
    }
    setIsPausing(false);
  };

  const handleUpdateConfiguration = async (timer: number, maxStrikes: number) => {
    if (!lobby?.lobbyId || isSavingConfig) return;
    setActionError(null);
    setIsSavingConfig(true);
    const result = await updateConfigurationAction(lobby.lobbyId, { timer, maxStrikes });
    if (result.error) {
      setActionError(getErrorMessage(result.error));
    } else {
      await refreshLobby();
    }
    setIsSavingConfig(false);
  };

  useEffect(() => {
    if (lobby && playerId !== null && !lobby.players.some((p) => p.id === playerId)) {
      removeSavedLobby(lobby.lobbyId);
      router.push("/");
      return;
    }
  }, [lobby, playerId, router]);

  return {
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
  };
}