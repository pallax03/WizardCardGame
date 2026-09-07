"use server";

import { LobbyApiResponse, LOBBY_ERRORS } from "@/features/lobby/types";
import { safeApiFetch } from "@/lib/api/api";
import {
  authHeadersForLobby,
  clearClientSecretCookie,
  setClientSecretCookie,
} from "@/lib/auth/clientSecret";
import { redirect } from "next/navigation";

interface AuthLobbyPlayerResponse {
  lobbyId: string;
  playerId: number;
  secret?: string;
}


export async function createLobbyAction(
  username: string
): Promise<{ error?: string } | void> {
  const trimmedName = username?.trim();
  if (!trimmedName) {
    return { error: LOBBY_ERRORS.EMPTY_USERNAME };
  }

  const { data, error } = await safeApiFetch<AuthLobbyPlayerResponse>(
    "/api/lobby",
    {
      method: "POST",
      body: { name: trimmedName, bot: null },
    }
  );

  if (error || !data) {
    return { error: error || LOBBY_ERRORS.CREATE_FAILED };
  }

  if (data.secret) {
    await setClientSecretCookie(data.lobbyId, data.secret);
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

  const { data, error } = await safeApiFetch<AuthLobbyPlayerResponse>(
    `/api/lobby/${trimmedLobbyId}`,
    {
      method: "POST",
      body: { name: trimmedName, bot: null },
    }
  );

  if (error || !data) {
    return { error: error || LOBBY_ERRORS.LOBBY_NOT_FOUND };
  }

  const resolvedLobbyId = data.lobbyId || trimmedLobbyId;
  if (data.secret) {
    await setClientSecretCookie(resolvedLobbyId, data.secret);
  }

  const pId = data.playerId !== undefined ? `&playerId=${data.playerId}` : "";
  const pName = `&playerName=${encodeURIComponent(trimmedName)}`;

  redirect(`/lobby/${resolvedLobbyId}?${pId}${pName}`);
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
    headers: await authHeadersForLobby(lobbyId),
    body: { lobbyId, playerId },
  });

  if (error) {
    return { error: LOBBY_ERRORS.LEAVE_FAILED };
  }

  await clearClientSecretCookie(lobbyId);

  return { success: true };
}

export async function startGameAction(
  lobbyId: string
): Promise<{ success?: boolean; error?: string }> {
  const { error } = await safeApiFetch(`/api/lobby/${lobbyId}/start`, {
    method: "POST",
    headers: await authHeadersForLobby(lobbyId),
  });

  if (error) {
    return { error };
  }

  return { success: true };
}
