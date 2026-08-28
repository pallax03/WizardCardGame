"use server";

import { redirect } from "next/navigation";
import { apiFetch } from "@/features/lobby/actions/api";
import { LobbyApiResponse, LOBBY_ERRORS } from "@/features/lobby/types";

export async function createLobbyAction(username: string) {
  const trimmedName = username?.trim();
  if (!trimmedName) {
    return { error: LOBBY_ERRORS.EMPTY_USERNAME };
  }

  const { data, error } = await apiFetch<{ lobbyId: string; playerId: number }>("/api/lobby", {
    method: "POST",
    body: { name: trimmedName, bot: null },
  });

  if (error || !data) {
    return { error: error || LOBBY_ERRORS.CREATE_FAILED };
  }

  redirect(`/lobby/${data.lobbyId}?playerId=${data.playerId}`);
}

export async function joinLobbyAction(username: string, lobbyId: string) {
  const trimmedName = username?.trim();
  const trimmedLobbyId = lobbyId?.trim();

  if (!trimmedName) {
    return { error: LOBBY_ERRORS.EMPTY_USERNAME };
  }
  if (!trimmedLobbyId) {
    return { error: LOBBY_ERRORS.EMPTY_LOBBY_ID };
  }

  const { data, error } = await apiFetch<{ lobbyId?: string; playerId?: number }>(
    `/api/lobby/${trimmedLobbyId}`,
    {
      method: "POST",
      body: { name: trimmedName, bot: null },
    }
  );

  if (error || !data) {
    return { error: LOBBY_ERRORS.LOBBY_NOT_FOUND };
  }

  const pId = data.playerId ? `&playerId=${data.playerId}` : "";
  const pName = `&playerName=${encodeURIComponent(trimmedName)}`;

  redirect(`/lobby/${data.lobbyId || trimmedLobbyId}?${pId}${pName}`);
}

export async function getLobbyAction(
  lobbyId: string
): Promise<{ data?: LobbyApiResponse; error?: string }> {
  return apiFetch<LobbyApiResponse>(`/api/lobby/${lobbyId}`, {
    method: "GET",
    cache: "no-store",
  });
}