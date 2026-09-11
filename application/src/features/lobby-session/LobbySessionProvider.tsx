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
  useState,
} from "react";
import { useParams, useRouter } from "next/navigation";

import type { ChatMessage } from "@/features/chat/types";
import { getLobbyState, getLobbyWsSecret } from "./api";
import { connectLobbySocket, type LobbySocket } from "./lobbySocket";
import { clearStoredSession, readStoredSession, writeStoredSession } from "./storage";
import type {
  LobbySessionAction,
  LobbySessionState,
  ServerEvent,
} from "./types";

const initialState = (lobbyId: string): LobbySessionState => ({
  lobbyId,
  playerId: null,
  connectionState: "connecting",
  lobby: null,
  connectedPlayerIds: [],
  messages: [],
  error: null,
});

function sessionReducer(state: LobbySessionState, action: LobbySessionAction): LobbySessionState {
  switch (action.type) {
    case "session/reset":
      return initialState(action.lobbyId);
    case "identity/resolved":
      return { ...state, playerId: action.playerId };
    case "connection/changed": {
      const isNowOpen = action.connectionState === "open";
      let newConnectedIds = state.connectedPlayerIds;
      if (state.playerId !== null) {
        const idSet = new Set(state.connectedPlayerIds);
        if (isNowOpen) idSet.add(state.playerId);
        else idSet.delete(state.playerId);
        newConnectedIds = Array.from(idSet);
      }
      return { ...state, connectionState: action.connectionState, connectedPlayerIds: newConnectedIds };
    }
    case "lobby/loaded": {
      const httpOnlineIds = new Set(
        action.lobby.players.filter((p) => p.isOnline).map((p) => p.id)
      );
      if (state.connectionState === "open" && state.playerId !== null) {
        httpOnlineIds.add(state.playerId);
      }
      return {
        ...state,
        lobby: action.lobby,
        connectedPlayerIds: Array.from(httpOnlineIds),
        error: null,
      };
    }
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
      let updatedLobby = state.lobby;
      if (
        action.event.type === "event" &&
        (action.event.event.action === "GameStarted" ||
          action.event.event.action === "GameResumed") &&
        state.lobby
      ) {
        updatedLobby = {
          ...state.lobby,
          status: "IN_GAME",
        };
      }
      return {
        ...state,
        lobby: updatedLobby,
        messages: [...state.messages, action.event],
        connectedPlayerIds,
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
};

const LobbySessionContext = createContext<LobbySessionContextValue | null>(null);

const MAX_RECONNECT_ATTEMPTS = 10;

export function LobbySessionProvider({ children }: PropsWithChildren) {
  const params = useParams();
  const router = useRouter();
  const lobbyId = params.id as string;
  const [state, dispatch] = useReducer(sessionReducer, lobbyId, initialState);
  const socketRef = useRef<LobbySocket | null>(null);
  const lobbyRequestRef = useRef<Promise<void> | null>(null);
  const lobbyRefreshQueuedRef = useRef(false);
  const [wsAuth, setWsAuth] = useState<{ lobbyId: string; secret: string } | null>(null);
  const wsSecret = wsAuth !== null && wsAuth.lobbyId === lobbyId ? wsAuth.secret : null;

  useEffect(() => {
    if (state.lobbyId === lobbyId) return;
    queueMicrotask(() => dispatch({ type: "session/reset", lobbyId }));
  }, [lobbyId, state.lobbyId]);

  useEffect(() => {
    const urlPlayerId = new URLSearchParams(window.location.search).get("playerId");
    const stored = readStoredSession();
    const candidate =
      urlPlayerId ?? (stored.lobbyId === lobbyId && stored.playerId !== null ? String(stored.playerId) : null);
    const playerId = candidate === null ? Number.NaN : Number.parseInt(candidate, 10);

    if (!Number.isInteger(playerId) || playerId < 0) {
      clearStoredSession();
      router.replace("/");
      return;
    }

    if (urlPlayerId) {
      writeStoredSession(lobbyId, urlPlayerId);
      if (typeof window !== "undefined" && !window.location.pathname.endsWith("/game")) {
        router.replace(`/lobby/${lobbyId}`);
      }
    }

    queueMicrotask(() => dispatch({ type: "identity/resolved", playerId }));
  }, [lobbyId, router]);

  useEffect(() => {
    let cancelled = false;
    void getLobbyWsSecret(lobbyId).then((secret) => {
      if (cancelled) return;
      if (!secret) {
        dispatch({
          type: "sync/failed",
          error: new Error("Missing lobby secret: rejoin the lobby from the home page."),
        });
        return;
      }
      setWsAuth({ lobbyId, secret });
    });
    return () => {
      cancelled = true;
    };
  }, [lobbyId]);

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

  useEffect(() => {
    void refreshLobby();
  }, [refreshLobby]);

  const handleServerEvent = useCallback((event: ServerEvent) => {
    dispatch({ type: "event/received", event });
    if (event.type === "system") {
      void refreshLobby();
      return;
    }

    if (event.type === "event") {
      if (event.event.action === "GameStarted" || event.event.action === "GameResumed") {
        void refreshLobby();
      }
    }
  }, [refreshLobby]);

  useEffect(() => {
    if (state.lobby?.status === "IN_GAME") {
      const targetPath = `/lobby/${state.lobbyId}/game`;

      if (typeof window !== "undefined" && window.location.pathname !== targetPath) {
        router.replace(targetPath);
      }
    }
  }, [state.lobby?.status, state.lobbyId, router]);

  useEffect(() => {
    if (state.lobby?.status === "PAUSED") {
      const targetPath = `/lobby/${state.lobbyId}`;

      if (typeof window !== "undefined" && window.location.pathname !== targetPath) {
        router.replace(targetPath);
      }
    }
  }, [state.lobby?.status, state.lobbyId, router]);

  useEffect(() => {
    if (state.playerId === null || wsSecret === null) return;

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
        secret: wsSecret,
        onEvent: handleServerEvent,
        onConnectionChange(connectionState) {
          dispatch({ type: "connection/changed", connectionState });
          if (connectionState === "open") {
            reconnectAttempt = 0;
            if (hasConnected) {
              void refreshLobby();
            }
            hasConnected = true;
          }
        },
        onClose() {
          if (disposed) return;
          reconnectAttempt += 1;
          if (reconnectAttempt > MAX_RECONNECT_ATTEMPTS) {
            dispatch({ type: "connection/changed", connectionState: "closed" });
            return;
          }
          dispatch({ type: "connection/changed", connectionState: "reconnecting" });
          const backoff = Math.min(500 * 2 ** (reconnectAttempt - 1), 5000);
          const delay = backoff * (0.5 + Math.random() * 0.5);
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
  }, [handleServerEvent, lobbyId, refreshLobby, state.playerId, wsSecret]);

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
  }), [refreshLobby, sendMessage, state]);

  return <LobbySessionContext.Provider value={value}>{children}</LobbySessionContext.Provider>;
}

export function useLobbySession() {
  const session = useContext(LobbySessionContext);
  if (!session) throw new Error("useLobbySession must be used inside LobbySessionProvider");
  return session;
}
