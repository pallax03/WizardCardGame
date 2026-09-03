"use server";

import { apiFetch } from "@/lib/api/api";
import type { LobbyState, GameState } from "./types";

export async function getLobbyState(lobbyId: string): Promise<LobbyState> {
  return await apiFetch<LobbyState>(`/api/lobby/${lobbyId}`, { cache: "no-store" });
}

export async function getGameState(_lobbyId: string, _playerId: number): Promise<GameState | null> {
  //TO-DO
  //return await apiFetch<GameState>(`/game/${_lobbyId}/player/${_playerId}`);
  return null;
}
