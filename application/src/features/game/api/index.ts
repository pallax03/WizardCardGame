"use server";

import { apiFetch } from "@/lib/api/api";
import type { Card, CardColor } from "../types";

export async function chooseTrumpColor(
  lobbyId: string,
  playerId: number,
  color: CardColor
): Promise<void> {
  await apiFetch(`/api/lobby/${lobbyId}/player/${playerId}/choose`, {
    method: "POST",
    body: color,
  });
}

export async function playCard(
  lobbyId: string,
  playerId: number,
  card: Card = { type: "Standard", color: "Blue", rank: 7 }
): Promise<void> {
  await apiFetch(`/api/lobby/${lobbyId}/player/${playerId}/play`, {
    method: "POST",
    body: card,
  });
}

export async function placeBid(
  lobbyId: string,
  playerId: number,
  bid: number
): Promise<void> {
  await apiFetch(`/api/lobby/${lobbyId}/player/${playerId}/place`, {
    method: "POST",
    body: bid,
  });
}
