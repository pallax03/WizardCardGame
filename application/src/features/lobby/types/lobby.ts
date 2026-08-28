import { LobbyPlayer } from "@/features/lobby-session/types";

export interface LobbyApiResponse {
  lobbyId: string;
  players: LobbyPlayer[];
}

export interface ApiOptions extends Omit<RequestInit, "body"> {
  body?: unknown;
}

export interface ApiResponse<T = unknown> {
  data?: T;
  error?: string;
  status?: number;
}