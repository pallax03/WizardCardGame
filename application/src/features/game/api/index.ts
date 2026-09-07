"use server";

import { apiFetch } from "@/lib/api/api";
import { authHeadersForLobby } from "@/lib/auth/clientSecret";
import type { Card, CardColor } from "../types";

export async function chooseTrumpColor(lobbyId: string, color: CardColor): Promise<void> {
  await apiFetch(`/api/lobby/${lobbyId}/choose`, {
    method: "POST",
    headers: await authHeadersForLobby(lobbyId),
    body: color,
  });
}

export async function playCard(
  lobbyId: string,
  card: Card = { type: "Standard", color: "Blue", rank: 7 }
): Promise<void> {
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
