import type { AnyMessage, ChatMessage, EventMessage, SystemMessage } from "@/features/chat/types";

export type ConnectionState = "connecting" | "open" | "reconnecting" | "closed";

export type LobbyPlayer = {
  id: number;
  name: string;
  difficulty?: string | null;
  isOnline?: boolean;
};

export type LobbyState = {
  lobbyId: string;
  players: LobbyPlayer[];
  status?: "WAITING" | "IN_GAME" | "DISCONNECTING" | "PAUSED" | "FINISHED";
  configuration?: unknown; //todo: add in getLobbyState endpoint GameConfiguration type
};

export type ServerEvent = ChatMessage | SystemMessage | EventMessage;

export type LobbySessionState = {
  lobbyId: string;
  playerId: number | null;
  connectionState: ConnectionState;
  lobby: LobbyState | null;
  connectedPlayerIds: number[];
  messages: AnyMessage[];
  error: Error | null;
};

export type LobbySessionAction =
  | { type: "session/reset"; lobbyId: string }
  | { type: "identity/resolved"; playerId: number }
  | { type: "connection/changed"; connectionState: ConnectionState }
  | { type: "lobby/loaded"; lobby: LobbyState }
  | { type: "event/received"; event: ServerEvent }
  | { type: "chat/privateSent"; message: ChatMessage }
  | { type: "sync/failed"; error: Error };
