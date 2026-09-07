"use client";

import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { useLobbySession } from "@/features/lobby-session";
import type { EventMessage } from "@/features/chat/types";
import { chooseTrumpColor, getPlayerGameSnapshot, placeBid, playCard } from "../api";
import { gameReducer, initialGameBoardState, isCardInList } from "../state/gameReducer";
import { mapSnapshotToBoardState } from "../state/snapshotMapper";
import type { Card, CardColor, GameBoardState } from "../types";

export function useGameBoard(customPlayerId?: number) {
  const {
    messages,
    playerId: sessionPlayerId,
    lobby,
    lobbyId,
    connectionState,
    connectedPlayerIds,
  } = useLobbySession();

  const resolvedPlayerId = customPlayerId ?? sessionPlayerId;
  const playerId = resolvedPlayerId ?? 1;

  // Local selection states for user interaction
  const [selectedCard, setSelectedCard] = useState<Card | null>(null);
  const [bidInput, setBidInput] = useState<number>(0);
  const [selectedColor, setSelectedColor] = useState<CardColor>("Red");
  const [isSubmitting, setIsSubmitting] = useState<boolean>(false);
  const [actionStatus, setActionStatus] = useState<string | null>(null);

  // Snapshot ripristinato dal backend (GET /api/lobby/{lobbyId}/game).
  // Serve da base quando la cronologia WS e' andata persa (reload/chiusura
  // finestra). Gli eventi arrivati dopo il fetch vengono ridotti sopra.
  const [snapshotState, setSnapshotState] = useState<GameBoardState | null>(null);
  const [snapshotBaseline, setSnapshotBaseline] = useState(0);
  const [isRestoring, setIsRestoring] = useState(false);
  const [snapshotError, setSnapshotError] = useState<string | null>(null);

  // Filter all EventMessage received from WebSocket
  const gameEvents = useMemo(
    () => messages.filter((m): m is EventMessage => m.type === "event"),
    [messages]
  );

  const gameEventsLengthRef = useRef(0);
  const snapshotRequestIdRef = useRef(0);
  const hasConnectedRef = useRef(false);
  const hasSnapshotRef = useRef(false);

  useEffect(() => {
    gameEventsLengthRef.current = gameEvents.length;
  }, [gameEvents.length]);

  useEffect(() => {
    hasSnapshotRef.current = snapshotState !== null;
  }, [snapshotState]);

  const refreshSnapshot = useCallback(
    async (reason: string = "manual") => {
      if (resolvedPlayerId === null || resolvedPlayerId === undefined) return;
      if (!lobbyId) return;
      const requestId = (snapshotRequestIdRef.current += 1);
      // Baseline catturata a inizio fetch: gli eventi gia' bufferati sono
      // (quasi sempre) gia' inclusi nello snapshot del server e vanno saltati.
      const baselineAtStart = gameEventsLengthRef.current;
      setIsRestoring(true);
      setSnapshotError(null);
      try {
        const snapshot = await getPlayerGameSnapshot(lobbyId);
        if (snapshotRequestIdRef.current !== requestId) return;
        if (snapshot) {
          setSnapshotState(mapSnapshotToBoardState(snapshot, playerId));
          setSnapshotBaseline(baselineAtStart);
        } else {
          // Nessuna partita sul backend (lobby in attesa): si resta in
          // modalita' solo-eventi senza sporcare lo stato precedente.
          setSnapshotBaseline(baselineAtStart);
        }
      } catch (error) {
        if (snapshotRequestIdRef.current !== requestId) return;
        const msg = error instanceof Error ? error.message : String(error);
        setSnapshotError(`Ripristino stato fallito (${reason}): ${msg}`);
      } finally {
        if (snapshotRequestIdRef.current === requestId) setIsRestoring(false);
      }
    },
    [lobbyId, playerId, resolvedPlayerId]
  );

  // Reset snapshot quando si cambia lobby/player: la baseline sugli eventi
  // non sarebbe piu' valida.
  useEffect(() => {
    queueMicrotask(() => {
      snapshotRequestIdRef.current += 1;
      setSnapshotState(null);
      setSnapshotBaseline(0);
      setSnapshotError(null);
      setIsRestoring(false);
      hasConnectedRef.current = false;
    });
  }, [lobbyId, resolvedPlayerId]);

  // Primo caricamento: tenta subito il ripristino, anche se il WS non ha
  // ancora recapitato alcun evento (caso finestra richiusa e riaperta).
  useEffect(() => {
    if (resolvedPlayerId === null || resolvedPlayerId === undefined) return;
    queueMicrotask(() => {
      void refreshSnapshot("mount");
    });
  }, [refreshSnapshot, resolvedPlayerId]);

  // Riconnessione WS: quando la connessione torna "open" dopo un'interruzione,
  // la cronologia locale potrebbe essere incompleta -> re-fetch. Alla prima
  // apertura si rifetcha solo se il fetch iniziale non ha prodotto snapshot.
  useEffect(() => {
    if (connectionState !== "open") return;
    const isFirstOpen = !hasConnectedRef.current;
    hasConnectedRef.current = true;
    if (isFirstOpen && hasSnapshotRef.current) return;
    const reason = isFirstOpen ? "mount" : "reconnect";
    queueMicrotask(() => {
      void refreshSnapshot(reason);
    });
  }, [connectionState, refreshSnapshot]);

  // Altri casi critici: ritorno in foreground, rete di nuovo online, focus.
  useEffect(() => {
    const onForeground = () => {
      if (document.visibilityState === "visible") void refreshSnapshot("foreground");
    };
    const onOnline = () => void refreshSnapshot("online");
    document.addEventListener("visibilitychange", onForeground);
    window.addEventListener("online", onOnline);
    window.addEventListener("focus", onOnline);
    return () => {
      document.removeEventListener("visibilitychange", onForeground);
      window.removeEventListener("online", onOnline);
      window.removeEventListener("focus", onOnline);
    };
  }, [refreshSnapshot]);

  // Reduce all events sequentially to derive current GameBoardState.
  // Se esiste uno snapshot, si applicano sopra solo gli eventi successivi
  // al fetch (baseline); altrimenti si parte dallo stato iniziale.
  const eventBasedState = useMemo<GameBoardState>(() => {
    return gameEvents.reduce(
      (state, event) => gameReducer(state, event, playerId),
      initialGameBoardState
    );
  }, [gameEvents, playerId]);

  const gameState = useMemo<GameBoardState>(() => {
    if (!snapshotState) return eventBasedState;
    const tail = snapshotBaseline <= gameEvents.length
      ? gameEvents.slice(snapshotBaseline)
      : gameEvents;
    return tail.reduce((state, event) => gameReducer(state, event, playerId), snapshotState);
  }, [snapshotState, snapshotBaseline, gameEvents, eventBasedState, playerId]);

  // Derived helpers
  const isMyTurn = gameState.currentTurn.isMyTurn;
  const canChooseTrump = isMyTurn && gameState.currentTurn.actionType === "CHOOSE_TRUMP";
  const canBid = isMyTurn && gameState.currentTurn.actionType === "BID";
  const canPlay = isMyTurn && gameState.currentTurn.actionType === "PLAY_CARD";

  // Map of player id to name and metadata from lobby
  const playersMap = useMemo(() => {
    const map = new Map<
      number,
      { id: number; name: string; isBot: boolean; isOnline: boolean }
    >();
    (lobby?.players ?? []).forEach((p) => {
      map.set(p.id, {
        id: p.id,
        name: p.name,
        isBot: Boolean(p.difficulty),
        isOnline: connectedPlayerIds.includes(p.id),
      });
    });
    return map;
  }, [lobby?.players, connectedPlayerIds]);

  // Human-readable description of current turn
  const turnPrompt = useMemo(() => {
    const activeId = gameState.currentTurn.playerId;
    const activePlayerName =
      activeId !== null
        ? playersMap.get(activeId)?.name ?? `Player ${activeId}`
        : "Unknown";

    if (isMyTurn) {
      switch (gameState.currentTurn.actionType) {
        case "CHOOSE_TRUMP":
          return "It is your turn to choose the Trump color!";
        case "BID":
          return `It is your turn to place your bid for Round ${gameState.round}!`;
        case "PLAY_CARD":
          return "It is your turn to play a card!";
        default:
          return "Your turn!";
      }
    }

    if (activeId !== null) {
      switch (gameState.currentTurn.actionType) {
        case "CHOOSE_TRUMP":
          return `Waiting for ${activePlayerName} to choose Trump color...`;
        case "BID":
          return `Waiting for ${activePlayerName} to place a bid...`;
        case "PLAY_CARD":
          return `Waiting for ${activePlayerName} to play a card...`;
        default:
          return `Waiting for ${activePlayerName}...`;
      }
    }

    if (gameState.status === "WAITING") return "Waiting for game to begin...";
    if (gameState.status === "ROUND_SCORED") return "Round completed! Scoring in progress...";
    if (gameState.status === "GAME_ENDED") return "Game finished!";
    if (gameState.status === "ABORTED") return "Game aborted!";

    return "Waiting for server...";
  }, [gameState.currentTurn, gameState.round, gameState.status, isMyTurn, playersMap]);

  // Action: Choose Trump Color
  const handleChooseTrump = useCallback(
    async (color?: CardColor) => {
      const colorToChoose = color ?? selectedColor;
      try {
        setIsSubmitting(true);
        setActionStatus(`Choosing trump color ${colorToChoose}...`);
        await chooseTrumpColor(lobbyId, colorToChoose);
        setActionStatus(`Trump color chosen: ${colorToChoose}`);
      } catch (error) {
        const msg = error instanceof Error ? error.message : String(error);
        setActionStatus(`Error choosing trump: ${msg}`);
      } finally {
        setIsSubmitting(false);
      }
    },
    [lobbyId, selectedColor]
  );

  // Action: Place Bid
  const handlePlaceBid = useCallback(
    async (bid?: number) => {
      const bidToPlace = bid !== undefined ? bid : bidInput;
      try {
        setIsSubmitting(true);
        setActionStatus(`Placing bid ${bidToPlace}...`);
        await placeBid(lobbyId, bidToPlace);
        setActionStatus(`Bid placed successfully: ${bidToPlace}`);
      } catch (error) {
        const msg = error instanceof Error ? error.message : String(error);
        setActionStatus(`Error placing bid: ${msg}`);
      } finally {
        setIsSubmitting(false);
      }
    },
    [bidInput, lobbyId]
  );

  // Action: Play Card
  const handlePlayCard = useCallback(
    async (card?: Card) => {
      // Default to selected card, or first legal card, or first hand card, or standard card
      const cardToPlay =
        card ??
        selectedCard ??
        gameState.legalCards[0] ??
        gameState.hand[0] ?? { type: "Standard", color: "Blue", rank: 7 };

      try {
        setIsSubmitting(true);
        setActionStatus("Playing card...");
        await playCard(lobbyId, cardToPlay);
        setActionStatus("Card played successfully");
        setSelectedCard(null);
      } catch (error) {
        const msg = error instanceof Error ? error.message : String(error);
        setActionStatus(`Error playing card: ${msg}`);
      } finally {
        setIsSubmitting(false);
      }
    },
    [gameState.hand, gameState.legalCards, lobbyId, selectedCard]
  );

  // Helper to check if a specific card in hand is playable
  const isCardPlayable = useCallback(
    (card: Card) => {
      if (!canPlay) return false;
      if (gameState.legalCards.length === 0) return true;
      return isCardInList(card, gameState.legalCards);
    },
    [canPlay, gameState.legalCards]
  );

  return {
    lobbyId,
    playerId,
    connectionState,
    lobby,
    playersMap,
    gameState,
    gameEvents,
    // Snapshot restore state
    isRestoring,
    snapshotError,
    hasSnapshot: snapshotState !== null,
    refreshSnapshot,
    // Turn & capability flags
    isMyTurn,
    canChooseTrump,
    canBid,
    canPlay,
    turnPrompt,
    isCardPlayable,
    // Interactive states
    selectedCard,
    setSelectedCard,
    bidInput,
    setBidInput,
    selectedColor,
    setSelectedColor,
    isSubmitting,
    actionStatus,
    setActionStatus,
    // Operations
    handleChooseTrump,
    handlePlaceBid,
    handlePlayCard,
  };
}
