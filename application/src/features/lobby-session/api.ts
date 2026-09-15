"use server";

import { apiFetch } from "@/lib/api/api";
import { getClientSecretCookie } from "@/lib/auth/clientSecret";
import type { LobbyState } from "./types";

export async function getLobbyState(lobbyId: string): Promise<LobbyState> {
  return await apiFetch<LobbyState>(`/api/lobby/${lobbyId}`, { cache: "no-store" });
}

/**
 * Restituisce il secret del player per autenticare il WebSocket.
 * Il secret vive in un cookie httpOnly (non leggibile da JS), mentre
 * l'engine richiede `GET /lobby/{lobbyId}?secret=...` per l'upgrade WS
 * (`WebSocketsVerticle`): i WebSocket browser non possono inviare
 * header `Authorization`, quindi il secret va passato in query string.
 */
export async function getLobbyWsSecret(lobbyId: string): Promise<string | undefined> {
  return await getClientSecretCookie(lobbyId);
}
