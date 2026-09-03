"use server";

import { apiFetch } from "@/lib/api/api";

export async function chooseTrumpColor(
  lobbyId: string,
  playerId: number,
  color: string
): Promise<void> {
  await apiFetch(`/api/lobby/${lobbyId}/player/${playerId}/choose`, {
    method: "POST",
    body: {
      action: "ResolveTrumpColor",
      playerId,
      color,
    },
  });
}

export async function playCard(
  lobbyId: string,
  playerId: number,
  card: unknown = { type: "Standard", color: "Blue", rank: 7 }
): Promise<void> {
  await apiFetch(`/api/lobby/${lobbyId}/player/${playerId}/play`, {
    method: "POST",
    body: {
      action: "PlayCard",
      playerId,
      card,
    },
  });
}

export async function placeBid(
  lobbyId: string,
  playerId: number,
  bid: number
): Promise<void> {
  await apiFetch(`/api/lobby/${lobbyId}/player/${playerId}/place`, {
    method: "POST",
    body: {
      action: "PlaceBid",
      playerId,
      bid,
    },
  });
}