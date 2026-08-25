"use server";

import { redirect } from "next/navigation";
import { LobbyApiResponse } from "@/features/lobby/types/lobby-types";

export async function createLobbyAction(username: string) {
  if (!username || !username.trim()) {
    return { error: "Inserisci un nome utente per continuare." };
  }

  const baseUrl = process.env.BACKEND_URL || process.env.NEXT_PUBLIC_BACKEND_URL;
  let destination = "";

  try {
    const res = await fetch(`${baseUrl}/api/lobby`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ name: username.trim(), bot: null }),
    });

    if (!res.ok) {
      const errorData = await res.json().catch(() => ({}));
      return { error: errorData.message || `Errore server: ${res.status}` };
    }

    const data = await res.json();
    destination = `/lobby/${data.lobbyId}?playerId=${data.playerId}`;
  } catch (err: any) {
    return { error: "Errore di connessione al server backend." };
  }

  redirect(destination);
}

export async function joinLobbyAction(username: string, lobbyId: string) {
  if (!username || !username.trim()) {
    return { error: "Inserisci il tuo nome prima di unirti." };
  }
  if (!lobbyId || !lobbyId.trim()) {
    return { error: "Inserisci il codice della stanza." };
  }

  const baseUrl = process.env.BACKEND_URL || process.env.NEXT_PUBLIC_BACKEND_URL;
  let destination = "";

  try {
    const res = await fetch(`${baseUrl}/api/lobby/${lobbyId.trim()}`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ name: username.trim(), bot: null }),
    });

    if (!res.ok) {
      return { error: "Stanza non trovata o piena." };
    }

    const data = await res.json();
    
    const pId = data.playerId ? `&playerId=${data.playerId}` : "";
    const pName = `&playerName=${encodeURIComponent(username.trim())}`;

    destination = `/lobby/${data.lobbyId || lobbyId}?${pId}${pName}`;
  } catch (err: any) {
    return { error: "Errore durante l'accesso alla stanza." };
  }

  redirect(destination);
}

export async function getLobbyAction(lobbyId: string): Promise<{ data?: LobbyApiResponse; error?: string }> {
  const baseUrl = process.env.BACKEND_URL || process.env.NEXT_PUBLIC_BACKEND_URL;

  try {
    const res = await fetch(`${baseUrl}/api/lobby/${lobbyId}`, {
      method: "GET",
      headers: { "Content-Type": "application/json" },
      cache: "no-store", 
    });

    if (!res.ok) {
      return { error: `Errore server backend (${res.status})` };
    }

    const data: LobbyApiResponse = await res.json();
    return { data };
  } catch (err: any) {
    console.error("Fetch lobby error:", err);
    return { error: "Impossibile connettersi al server backend." };
  }
}

export async function addBotAction(
  lobbyId: string,
  botDifficulty: "Dumb" | "Prolog"
): Promise<{ success?: boolean; error?: string }> {
  const baseUrl = process.env.BACKEND_URL || process.env.NEXT_PUBLIC_BACKEND_URL;
  console.log(`Adding bot to lobby ${lobbyId} with difficulty ${botDifficulty}`);
  try {
    const res = await fetch(`${baseUrl}/api/lobby/${lobbyId}`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({name: "", bot: botDifficulty }),
    });

    if (!res.ok) {
      const errorData = await res.json().catch(() => ({}));
      return { error: errorData.message || `Errore nell'aggiunta del bot (${res.status})` };
    }

    return { success: true };
  } catch (err: any) {
    console.error("Add bot error:", err);
    return { error: "Errore di connessione al server backend durante l'aggiunta del bot." };
  }
}

export async function leaveLobbyAction(
  lobbyId: string,
  playerId: number
): Promise<{ success?: boolean; error?: string }> {
  const baseUrl = process.env.BACKEND_URL || process.env.NEXT_PUBLIC_BACKEND_URL;

  try {
    const res = await fetch(`${baseUrl}/api/lobby`, {
      method: "DELETE",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ lobbyId, playerId }),
    });

    if (!res.ok) {
      const errorData = await res.json().catch(() => ({}));
      return { error: errorData.message || `Errore durante l'uscita dalla lobby (${res.status})` };
    }

    return { success: true };
  } catch (err: any) {
    console.error("Leave lobby error:", err);
    return { error: "Errore di connessione al server backend durante l'uscita." };
  }
}