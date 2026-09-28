const SAVED_LOBBIES_KEY = "wizard_saved_lobbies";
// Chiavi legacy (singola sessione): migrate in automatico alla prima lettura.
const LEGACY_LOBBY_ID_KEY = "wizard_lobbyId";
const LEGACY_PLAYER_ID_KEY = "wizard_playerId";
const LEGACY_VOLUNTARY_LEAVE_KEY = "wizard_voluntary_leave";

export interface SavedLobby {
  lobbyId: string;
  playerId: number;
  savedAt: number;
}

function storageAvailable(): boolean {
  return typeof window !== "undefined";
}

function parsePlayerId(raw: string | null): number | null {
  const parsed = raw !== null ? Number(raw) : NaN;
  return Number.isInteger(parsed) && parsed >= 0 ? parsed : null;
}

/** Migra una sola volta la vecchia sessione singola nella nuova lista. */
function migrateLegacy(): void {
  if (!storageAvailable()) return;
  if (localStorage.getItem(SAVED_LOBBIES_KEY) !== null) return;
  const lobbyId = localStorage.getItem(LEGACY_LOBBY_ID_KEY);
  const playerId = parsePlayerId(localStorage.getItem(LEGACY_PLAYER_ID_KEY));
  if (lobbyId && playerId !== null) {
    persist([{ lobbyId, playerId, savedAt: Date.now() }]);
  }
  localStorage.removeItem(LEGACY_LOBBY_ID_KEY);
  localStorage.removeItem(LEGACY_PLAYER_ID_KEY);
  localStorage.removeItem(LEGACY_VOLUNTARY_LEAVE_KEY);
}

function persist(lobbies: SavedLobby[]): void {
  localStorage.setItem(SAVED_LOBBIES_KEY, JSON.stringify(lobbies));
}

/**
 * Lista delle lobby in cui il giocatore è ancora membro (partite in pausa
 * incluse): il secret resta nel cookie httpOnly, qui basta lobbyId+playerId
 * per ritrovarle e riconnettersi dalla home.
 */
export function readSavedLobbies(): SavedLobby[] {
  if (!storageAvailable()) return [];
  migrateLegacy();
  try {
    const raw = localStorage.getItem(SAVED_LOBBIES_KEY);
    if (!raw) return [];
    const parsed: unknown = JSON.parse(raw);
    if (!Array.isArray(parsed)) return [];
    return parsed.filter(
      (entry): entry is SavedLobby =>
        typeof entry === "object" &&
        entry !== null &&
        typeof (entry as SavedLobby).lobbyId === "string" &&
        Number.isInteger((entry as SavedLobby).playerId) &&
        (entry as SavedLobby).playerId >= 0
    );
  } catch {
    return [];
  }
}

export function findSavedLobby(lobbyId: string): SavedLobby | null {
  return readSavedLobbies().find((entry) => entry.lobbyId === lobbyId) ?? null;
}

/** Salva (o aggiorna) una lobby in lista: usato a ogni join e al pulsante indietro. */
export function saveLobby(lobbyId: string, playerId: string | number): void {
  if (!storageAvailable()) return;
  const parsed = typeof playerId === "number" ? playerId : Number(playerId);
  if (!Number.isInteger(parsed) || parsed < 0) return;
  const others = readSavedLobbies().filter((entry) => entry.lobbyId !== lobbyId);
  persist([...others, { lobbyId, playerId: parsed, savedAt: Date.now() }]);
}

/** Rimuove una lobby dalla lista: abbandono reale (DELETE) o lobby non più esistente. */
export function removeSavedLobby(lobbyId: string): void {
  if (!storageAvailable()) return;
  persist(readSavedLobbies().filter((entry) => entry.lobbyId !== lobbyId));
}
