"use server";

import { apiFetch } from "@/features/lobby/actions/api";
import { LOBBY_ERRORS } from "@/features/lobby/types";

export async function addBotAction(
  lobbyId: string,
  botDifficulty: "Dumb" | "Prolog"
): Promise<{ success?: boolean; error?: string }> {
  const { error } = await apiFetch(`/api/lobby/${lobbyId}`, {
    method: "POST",
    body: { name: "", bot: botDifficulty },
  });

  if (error) {
    return { error: LOBBY_ERRORS.ADD_BOT_FAILED };
  }

  return { success: true };
}

export async function leaveLobbyAction(
  lobbyId: string,
  playerId: number
): Promise<{ success?: boolean; error?: string }> {
  const { error } = await apiFetch("/api/lobby", {
    method: "DELETE",
    body: { lobbyId, playerId },
  });

  if (error) {
    return { error: LOBBY_ERRORS.LEAVE_FAILED };
  }

  return { success: true };
}

export async function startGameAction(
  lobbyId: string
): Promise<{ success?: boolean; error?: string }> {
  const { error } = await apiFetch(`/api/lobby/${lobbyId}/start`, {
    method: "POST",
  });

  if (error) {
    return { error };
  }

  return { success: true };
}