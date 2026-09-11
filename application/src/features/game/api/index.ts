"use server";

import { ApiError, apiFetch } from "@/lib/api/api";
import { authHeadersForLobby } from "@/lib/auth/clientSecret";
import type { PlayerGameSnapshot } from "../state/snapshotMapper";
import type { Card, CardColor } from "../types";

export async function getPlayerGameSnapshot(
  lobbyId: string
): Promise<PlayerGameSnapshot | null> {
  try {
    return await apiFetch<PlayerGameSnapshot>(`/api/lobby/${lobbyId}/game`, {
      cache: "no-store",
      headers: await authHeadersForLobby(lobbyId),
    });
  } catch (error) {
    if (error instanceof ApiError && error.status === 404) return null;
    throw error;
  }
}

export async function chooseTrumpColor(lobbyId: string, color: CardColor): Promise<void> {
  await apiFetch(`/api/lobby/${lobbyId}/choose`, {
    method: "POST",
    headers: await authHeadersForLobby(lobbyId),
    body: color,
  });
}

export async function playCard(lobbyId: string, card: Card): Promise<void> {
  await apiFetch(`/api/lobby/${lobbyId}/play`, {
    method: "POST",
    headers: await authHeadersForLobby(lobbyId),
    body: card,
  });
}

export async function placeBid(lobbyId: string, bid: number): Promise<void> {
  await apiFetch(`/api/lobby/${lobbyId}/place`, {
    method: "POST",
    headers: await authHeadersForLobby(lobbyId),
    body: bid,
  });
}
