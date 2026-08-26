import type { ChatMessage, EventMessage, SystemMessage } from "@/features/chat/types";
import type { ConnectionState, ServerEvent } from "./types";

type LobbySocketOptions = {
  lobbyId: string;
  playerId: number;
  onEvent: (event: ServerEvent) => void;
  onConnectionChange: (state: ConnectionState) => void;
  onClose: () => void;
};

export type LobbySocket = {
  send: (message: ChatMessage) => boolean;
  close: () => void;
};

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === "object" && value !== null;
}

function parseServerEvent(rawData: string): ServerEvent | null {
  let raw: unknown;
  try {
    raw = JSON.parse(rawData);
  } catch {
    return null;
  }

  if (!isRecord(raw)) return null;
  const timestamp = typeof raw.timestamp === "string" ? raw.timestamp : new Date().toISOString();

  if (raw.type === "message" && typeof raw.playerId === "number" && typeof raw.text === "string") {
    return {
      type: "message",
      playerId: raw.playerId,
      destinationId: typeof raw.destinationId === "number" ? raw.destinationId : undefined,
      text: raw.text,
      timestamp,
    } satisfies ChatMessage;
  }

  if (
    raw.type === "system" &&
    typeof raw.playerId === "number" &&
    (raw.action === "joined" || raw.action === "left" || raw.action === "online" || raw.action === "offline")
  ) {
    return {
      type: "system",
      playerId: raw.playerId,
      action: raw.action,
      timestamp,
    } satisfies SystemMessage;
  }

  if (isRecord(raw.event) && typeof raw.event.type === "string" && typeof raw.event.action === "string") {
    const event: EventMessage["event"] = {
      type: raw.event.type,
      action: raw.event.action,
      playerId: typeof raw.event.playerId === "number" ? raw.event.playerId : undefined,
      destinationId: typeof raw.event.destinationId === "number" ? raw.event.destinationId : undefined,
      fields: isRecord(raw.event.fields) ? raw.event.fields : undefined,
    };
    return { type: "event", event, timestamp } satisfies EventMessage;
  }

  return null;
}

export function connectLobbySocket({
  lobbyId,
  playerId,
  onEvent,
  onConnectionChange,
  onClose,
}: LobbySocketOptions): LobbySocket {
  const baseUrl = process.env.NEXT_PUBLIC_WS_URL || "ws://localhost:5002";
  const socket = new WebSocket(`${baseUrl}/lobby/${lobbyId}/player/${playerId}`);

  socket.onopen = () => onConnectionChange("open");
  socket.onmessage = ({ data }) => {
    if (typeof data !== "string") return;
    const event = parseServerEvent(data);
    if (event) onEvent(event);
  };
  socket.onerror = () => onConnectionChange("closed");
  socket.onclose = onClose;

  return {
    send(message) {
      if (socket.readyState !== WebSocket.OPEN) return false;
      socket.send(JSON.stringify(message));
      return true;
    },
    close() {
      socket.close(1000);
    },
  };
}
