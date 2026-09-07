"use server";

import { apiFetch } from "@/lib/api/api";
import { authHeadersForLobby } from "@/lib/auth/clientSecret";
import type { LobbyState, GameState } from "./types";

export async function getLobbyState(lobbyId: string): Promise<LobbyState> {
  return await apiFetch<LobbyState>(`/api/lobby/${lobbyId}`, { cache: "no-store" });
}

export async function getGameState(lobbyId: string): Promise<GameState | null> {
  return await apiFetch<GameState>(`/api/lobby/${lobbyId}/game`, {
    cache: "no-store",
    headers: await authHeadersForLobby(lobbyId),
  });
}
