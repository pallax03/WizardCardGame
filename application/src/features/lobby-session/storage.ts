const LOBBY_ID_KEY = "wizard_lobbyId";
const PLAYER_ID_KEY = "wizard_playerId";

export interface StoredSession {
  lobbyId: string | null;
  playerId: number | null;
}

function storageAvailable(): boolean {
  return typeof window !== "undefined";
}

export function readStoredSession(): StoredSession {
  if (!storageAvailable()) return { lobbyId: null, playerId: null };
  const lobbyId = localStorage.getItem(LOBBY_ID_KEY);
  const rawPlayerId = localStorage.getItem(PLAYER_ID_KEY);
  const parsed = rawPlayerId !== null ? Number(rawPlayerId) : NaN;
  return {
    lobbyId,
    playerId: Number.isInteger(parsed) && parsed >= 0 ? parsed : null,
  };
}

export function writeStoredSession(lobbyId: string, playerId: string | number): void {
  if (!storageAvailable()) return;
  localStorage.setItem(LOBBY_ID_KEY, lobbyId);
  localStorage.setItem(PLAYER_ID_KEY, String(playerId));
}

export function clearStoredSession(): void {
  if (!storageAvailable()) return;
  localStorage.removeItem(LOBBY_ID_KEY);
  localStorage.removeItem(PLAYER_ID_KEY);
}
