"use client";

import {
  createContext,
  type PropsWithChildren,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useReducer,
  useRef,
} from "react";
import { useParams, useRouter } from "next/navigation";

import type { ChatMessage } from "@/features/chat/types";
import { getGameState, getLobbyState } from "./api";
import { connectLobbySocket, type LobbySocket } from "./lobbySocket";
import type {
  LobbySessionAction,
  LobbySessionState,
  LobbyState,
  GameState,
  ServerEvent,
} from "./types";

const initialState = (lobbyId: string): LobbySessionState => ({
  lobbyId,
  playerId: null,
  connectionState: "connecting",
  lobby: null,
  connectedPlayerIds: [],
  game: null,
  messages: [],
  lastGameEvent: null,
  error: null,
});

function sessionReducer(state: LobbySessionState, action: LobbySessionAction): LobbySessionState {
  switch (action.type) {
    case "session/reset":
      return initialState(action.lobbyId);
    case "identity/resolved":
      return { ...state, playerId: action.playerId };
    case "connection/changed":
      return { ...state, connectionState: action.connectionState };
    case "lobby/loaded":
      return {
        ...state,
        lobby: action.lobby,
        connectedPlayerIds: state.lobby === null
          ? action.lobby.players.map((player) => player.id)
          : state.connectedPlayerIds,
        error: null,
      };
    case "game/loaded":
      return { ...state, game: action.game, error: null };
    case "event/received": {
      let connectedPlayerIds = state.connectedPlayerIds;
      if (action.event.type === "system") {
        const playerId = action.event.playerId;
        const evAction = action.event.action;
        if (evAction === "joined" || evAction === "online") {
          connectedPlayerIds = Array.from(new Set([...state.connectedPlayerIds, playerId]));
        } else if (evAction === "left" || evAction === "offline") {
          connectedPlayerIds = state.connectedPlayerIds.filter((id) => id !== playerId);
        }
      }
      return {
        ...state,
        messages: [...state.messages, action.event],
        connectedPlayerIds,
        lastGameEvent: action.event.type === "event" ? action.event.event : state.lastGameEvent,
      };
    }
    case "chat/privateSent":
      return { ...state, messages: [...state.messages, action.message] };
    case "sync/failed":
      return { ...state, error: action.error };
  }
}

type LobbySessionContextValue = LobbySessionState & {
  sendMessage: (text: string, destinationId?: number) => boolean;
  refreshLobby: () => Promise<void>;
  refreshGame: () => Promise<void>;
};

const LobbySessionContext = createContext<LobbySessionContextValue | null>(null);

export function LobbySessionProvider({ children }: PropsWithChildren) {
  const params = useParams();
  const router = useRouter();
  const lobbyId = params.id as string;
  const [state, dispatch] = useReducer(sessionReducer, lobbyId, initialState);
  const socketRef = useRef<LobbySocket | null>(null);
  const lobbyRequestRef = useRef<Promise<void> | null>(null);
  const gameRequestRef = useRef<Promise<void> | null>(null);
  const lobbyRefreshQueuedRef = useRef(false);
  const gameRefreshQueuedRef = useRef(false);
  const gameWasLoadedRef = useRef(false);

  useEffect(() => {
    if (state.lobbyId === lobbyId) return;
    queueMicrotask(() => dispatch({ type: "session/reset", lobbyId }));
  }, [lobbyId, state.lobbyId]);

  useEffect(() => {
    const urlPlayerId = new URLSearchParams(window.location.search).get("playerId");
    const storedPlayerId = localStorage.getItem("wizard_playerId");
    const storedLobbyId = localStorage.getItem("wizard_lobbyId");
    const candidate = urlPlayerId ?? (storedLobbyId === lobbyId ? storedPlayerId : null);
    const playerId = candidate === null ? Number.NaN : Number.parseInt(candidate, 10);

    if (Number.isNaN(playerId)) {
      router.replace("/");
      return;
    }

    if (urlPlayerId) {
      localStorage.setItem("wizard_playerId", urlPlayerId);
      localStorage.setItem("wizard_lobbyId", lobbyId);
      router.replace(`/lobby/${lobbyId}`);
    }

    queueMicrotask(() => dispatch({ type: "identity/resolved", playerId }));
  }, [lobbyId, router]);

  const refreshLobby = useCallback(() => {
    if (lobbyRequestRef.current) {
      lobbyRefreshQueuedRef.current = true;
      return lobbyRequestRef.current;
    }
    const request = (async () => {
      do {
        lobbyRefreshQueuedRef.current = false;
        try {
          const lobby = await getLobbyState(lobbyId);
          dispatch({ type: "lobby/loaded", lobby });
        } catch (reason: unknown) {
          const error = reason instanceof Error ? reason : new Error(String(reason));
          dispatch({ type: "sync/failed", error });
        }
      } while (lobbyRefreshQueuedRef.current);
      lobbyRequestRef.current = null;
    })();
    lobbyRequestRef.current = request;
    return request;
  }, [lobbyId]);

  const refreshGame = useCallback(() => {
    if (state.playerId === null) return Promise.resolve();
    if (gameRequestRef.current) {
      gameRefreshQueuedRef.current = true;
      return gameRequestRef.current;
    }
    const playerId = state.playerId;
    const request = (async () => {
      do {
        gameRefreshQueuedRef.current = false;
        try {
          const game = await getGameState(lobbyId, playerId);
          gameWasLoadedRef.current = true;
          dispatch({ type: "game/loaded", game });
        } catch (reason: unknown) {
          const error = reason instanceof Error ? reason : new Error(String(reason));
          dispatch({ type: "sync/failed", error });
        }
      } while (gameRefreshQueuedRef.current);
      gameRequestRef.current = null;
    })();
    gameRequestRef.current = request;
    return request;
  }, [lobbyId, state.playerId]);

  useEffect(() => {
    void refreshLobby();
  }, [refreshLobby]);

  const handleServerEvent = useCallback((event: ServerEvent) => {
    dispatch({ type: "event/received", event });

    if (event.type === "system") {
      if (event.action === "joined" || event.action === "left") {
        void refreshLobby();
      }
      return;
    }

    if (event.type === "event") {
      // Game reducers can progressively handle individual actions here. Until
      // then, refresh only when a game snapshot has already been requested.
      if (
        gameWasLoadedRef.current ||
        event.event.action === "GameStarted" ||
        event.event.action === "CardsDealt"
      ) {
        void refreshGame();
      }
      if (event.event.action === "GameStarted") void refreshLobby();
    }
  }, [refreshGame, refreshLobby]);

  useEffect(() => {
    if (state.playerId === null) return;

    let disposed = false;
    let reconnectTimer: ReturnType<typeof setTimeout> | undefined;
    let reconnectAttempt = 0;
    let hasConnected = false;

    const connect = () => {
      if (disposed) return;
      dispatch({
        type: "connection/changed",
        connectionState: reconnectAttempt === 0 ? "connecting" : "reconnecting",
      });

      socketRef.current = connectLobbySocket({
        lobbyId,
        playerId: state.playerId!,
        onEvent: handleServerEvent,
        onConnectionChange(connectionState) {
          dispatch({ type: "connection/changed", connectionState });
          if (connectionState === "open") {
            reconnectAttempt = 0;
            if (hasConnected) {
              void refreshLobby();
              if (gameWasLoadedRef.current) void refreshGame();
            }
            hasConnected = true;
          }
        },
        onClose() {
          if (disposed) return;
          dispatch({ type: "connection/changed", connectionState: "reconnecting" });
          reconnectAttempt += 1;
          const delay = Math.min(500 * 2 ** (reconnectAttempt - 1), 5000);
          reconnectTimer = setTimeout(connect, delay);
        },
      });
    };

    connect();
    return () => {
      disposed = true;
      if (reconnectTimer) clearTimeout(reconnectTimer);
      socketRef.current?.close();
      socketRef.current = null;
    };
  }, [handleServerEvent, lobbyId, refreshGame, refreshLobby, state.playerId]);

  const sendMessage = useCallback((text: string, destinationId?: number) => {
    if (state.playerId === null) return false;
    const message: ChatMessage = {
      type: "message",
      playerId: state.playerId,
      destinationId,
      text,
      timestamp: new Date().toISOString(),
    };
    const sent = socketRef.current?.send(message) ?? false;
    if (sent && destinationId !== undefined) {
      dispatch({ type: "chat/privateSent", message });
    }
    return sent;
  }, [state.playerId]);

  const value = useMemo<LobbySessionContextValue>(() => ({
    ...state,
    sendMessage,
    refreshLobby,
    refreshGame,
  }), [refreshGame, refreshLobby, sendMessage, state]);

  return <LobbySessionContext.Provider value={value}>{children}</LobbySessionContext.Provider>;
}

export function useLobbySession() {
  const session = useContext(LobbySessionContext);
  if (!session) throw new Error("useLobbySession must be used inside LobbySessionProvider");
  return session;
}

export function useLobbyState(): LobbyState | null {
  return useLobbySession().lobby;
}

export function useGameState(): GameState | null {
  return useLobbySession().game;
}

export function useLobbyPresence(): number[] {
  return useLobbySession().connectedPlayerIds;
}
