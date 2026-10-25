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

export async function getBestCardHint(lobbyId: string): Promise<Card> {
  return await apiFetch<Card>(`/api/lobby/${lobbyId}/hint/card`, {
    cache: "no-store",
    headers: await authHeadersForLobby(lobbyId),
  });
}

export async function getBestBidHint(lobbyId: string): Promise<number> {
  return await apiFetch<number>(`/api/lobby/${lobbyId}/hint/bid`, {
    cache: "no-store",
    headers: await authHeadersForLobby(lobbyId),
  });
}

export async function getBestTrumpHint(lobbyId: string): Promise<CardColor> {
  return await apiFetch<CardColor>(`/api/lobby/${lobbyId}/hint/choose`, {
    cache: "no-store",
    headers: await authHeadersForLobby(lobbyId),
  });
}
