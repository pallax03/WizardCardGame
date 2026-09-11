import type { LobbyPlayer } from "./types";

export function isBotPlayer(player: Pick<LobbyPlayer, "difficulty">): boolean {
  return player.difficulty !== undefined && player.difficulty !== null;
}

export function isPlayerOnline(player: LobbyPlayer, connectedPlayerIds: number[]): boolean {
  if (isBotPlayer(player)) return true;
  return connectedPlayerIds.includes(player.id);
}

export interface PlayerPresence {
  id: number;
  name: string;
  isBot: boolean;
  isOnline: boolean;
}

export function buildPlayersMap(
  players: LobbyPlayer[],
  connectedPlayerIds: number[]
): Map<number, PlayerPresence> {
  const map = new Map<number, PlayerPresence>();
  for (const player of players) {
    map.set(player.id, {
      id: player.id,
      name: player.name,
      isBot: isBotPlayer(player),
      isOnline: isPlayerOnline(player, connectedPlayerIds),
    });
  }
  return map;
}
