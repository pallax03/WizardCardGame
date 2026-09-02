"use server";

import type { LobbyState, GameState } from "./types";

async function getJson<T>(path: string): Promise<T> {
  const backendUrl = process.env.NEXT_PUBLIC_BACKEND_URL || "http://localhost:5001";
  const response = await fetch(`${backendUrl}${path}`, { cache: "no-store" });
  if (!response.ok) {
    throw new Error(`GET ${path} failed with status ${response.status}`);
  }
  return response.json() as Promise<T>;
}

export async function getLobbyState(lobbyId: string) {
  return await getJson<LobbyState>(`/api/lobby/${lobbyId}`);
}

export async function getGameState(lobbyId: string, playerId: number) {
  //TO-DO
  //return await getJson<GameState>(`/game/${lobbyId}/player/${playerId}`);
  return null;
}
