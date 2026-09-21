const LOBBY_ID_KEY = "wizard_lobbyId";
const PLAYER_ID_KEY = "wizard_playerId";
const VOLUNTARY_LEAVE_KEY = "wizard_voluntary_leave";

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
  if (localStorage.getItem(VOLUNTARY_LEAVE_KEY) === lobbyId) {
    localStorage.removeItem(VOLUNTARY_LEAVE_KEY);
  }
}

export function clearStoredSession(): void {
  if (!storageAvailable()) return;
  localStorage.removeItem(LOBBY_ID_KEY);
  localStorage.removeItem(PLAYER_ID_KEY);
}

export function markVoluntaryLeave(lobbyId: string): void {
  if (!storageAvailable()) return;
  localStorage.setItem(VOLUNTARY_LEAVE_KEY, lobbyId);
}

export function clearVoluntaryLeave(): void {
  if (!storageAvailable()) return;
  localStorage.removeItem(VOLUNTARY_LEAVE_KEY);
}

export function readVoluntaryLeave(): string | null {
  if (!storageAvailable()) return null;
  return localStorage.getItem(VOLUNTARY_LEAVE_KEY);
}
