"use server";

import { LobbyApiResponse, LOBBY_ERRORS } from "@/features/lobby/types";
import { safeApiFetch } from "@/lib/api/api";
import { redirect } from "next/navigation";


export async function createLobbyAction(
  username: string
): Promise<{ error?: string } | void> {
  const trimmedName = username?.trim();
  if (!trimmedName) {
    return { error: LOBBY_ERRORS.EMPTY_USERNAME };
  }

  const { data, error } = await safeApiFetch<{ lobbyId: string; playerId: number }>(
    "/api/lobby",
    {
      method: "POST",
      body: { name: trimmedName, bot: null },
    }
  );

  if (error || !data) {
    return { error: error || LOBBY_ERRORS.CREATE_FAILED };
  }

  redirect(`/lobby/${data.lobbyId}?playerId=${data.playerId}`);
}

export async function joinLobbyAction(
  username: string,
  lobbyId: string
): Promise<{ error?: string } | void> {
  const trimmedName = username?.trim();
  const trimmedLobbyId = lobbyId?.trim();

  if (!trimmedName) {
    return { error: LOBBY_ERRORS.EMPTY_USERNAME };
  }
  if (!trimmedLobbyId) {
    return { error: LOBBY_ERRORS.EMPTY_LOBBY_ID };
  }

  const { data, error } = await safeApiFetch<{ lobbyId?: string; playerId?: number }>(
    `/api/lobby/${trimmedLobbyId}`,
    {
      method: "POST",
      body: { name: trimmedName, bot: null },
    }
  );

  if (error || !data) {
    return { error: error || LOBBY_ERRORS.LOBBY_NOT_FOUND };
  }

  const pId = data.playerId !== undefined ? `&playerId=${data.playerId}` : "";
  const pName = `&playerName=${encodeURIComponent(trimmedName)}`;

  redirect(`/lobby/${data.lobbyId || trimmedLobbyId}?${pId}${pName}`);
}

export async function getLobbyAction(
  lobbyId: string
): Promise<{ data?: LobbyApiResponse; error?: string }> {
  return safeApiFetch<LobbyApiResponse>(`/api/lobby/${lobbyId}`, {
    method: "GET",
    cache: "no-store",
  });
}

export async function addBotAction(
  lobbyId: string,
  botDifficulty: "Dumb" | "Prolog"
): Promise<{ success?: boolean; error?: string }> {
  const { error } = await safeApiFetch(`/api/lobby/${lobbyId}`, {
    method: "POST",
    body: { name: "", bot: botDifficulty },
  });

  if (error) {
    return { error: LOBBY_ERRORS.ADD_BOT_FAILED };
  }

  return { success: true };
}

export async function leaveLobbyAction(
  lobbyId: string,
  playerId: number
): Promise<{ success?: boolean; error?: string }> {
  const { error } = await safeApiFetch("/api/lobby", {
    method: "DELETE",
    body: { lobbyId, playerId },
  });

  if (error) {
    return { error: LOBBY_ERRORS.LEAVE_FAILED };
  }

  return { success: true };
}

export async function startGameAction(
  lobbyId: string
): Promise<{ success?: boolean; error?: string }> {
  const { error } = await safeApiFetch(`/api/lobby/${lobbyId}/start`, {
    method: "POST",
  });

  if (error) {
    return { error };
  }

  return { success: true };
}
