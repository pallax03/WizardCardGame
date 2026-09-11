"use client";

import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { useLobbySession } from "@/features/lobby-session";
import { buildPlayersMap } from "@/features/lobby-session/presence";
import type { EventMessage } from "@/features/chat/types";
import { chooseTrumpColor, getPlayerGameSnapshot, placeBid, playCard } from "../api";
import {
  extractApiErrorCode,
  formatGameActionError,
  formatInvalidBidError,
  gameReducer,
  initialGameBoardState,
  isCardInList,
  parseInvalidBid,
  shortGameActionReason,
} from "../state/gameReducer";
import { mapSnapshotToBoardState } from "../state/snapshotMapper";
import type { Card, CardColor, GameBoardState, PlayedCardEntry } from "../types";

/** Secondi di pausa a fine presa: il tavolo resta visibile prima di pulirsi. */
export const TRICK_REVEAL_SECONDS = 4;

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
  const playerId: number | null = resolvedPlayerId ?? null;
  const reducerPlayerId = playerId ?? -1;

  const [selectedCard, setSelectedCard] = useState<Card | null>(null);
  const [bidInput, setBidInput] = useState<number>(0);
  const [selectedColor, setSelectedColor] = useState<CardColor>("Red");
  const [isSubmitting, setIsSubmitting] = useState<boolean>(false);
  const [actionStatus, setActionStatus] = useState<string | null>(null);
  const [bidErrorRaw, setBidErrorRaw] = useState<{ round: number; message: string } | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);
  const [bidAckRound, setBidAckRound] = useState<number | null>(null);
  const [snapshotState, setSnapshotState] = useState<GameBoardState | null>(null);
  const [snapshotBaseline, setSnapshotBaseline] = useState(0);
  const [isRestoring, setIsRestoring] = useState(false);
  const [snapshotError, setSnapshotError] = useState<string | null>(null);

  const [revealedTrick, setRevealedTrick] = useState<{
    entries: PlayedCardEntry[];
    winningCard: Card | null;
    winnerId: number;
  } | null>(null);
  const [revealSecondsLeft, setRevealSecondsLeft] = useState(0);
  const completedTableRef = useRef<{
    entries: PlayedCardEntry[];
    winningCard: Card | null;
  } | null>(null);
  const trickWonCountRef = useRef(0);
  const revealTimeoutRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  const revealIntervalRef = useRef<ReturnType<typeof setInterval> | null>(null);

  const gameEvents = useMemo(
    () => messages.filter((m): m is EventMessage => m.type === "event"),
    [messages]
  );

  const trickWonCount = useMemo(
    () => gameEvents.filter((e) => e.event.action === "TrickWon").length,
    [gameEvents]
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
      if (playerId === null || playerId === undefined) return;
      if (!lobbyId) return;
      const requestId = (snapshotRequestIdRef.current += 1);
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
    [lobbyId, playerId]
  );

  useEffect(() => {
    queueMicrotask(() => {
      snapshotRequestIdRef.current += 1;
      setSnapshotState(null);
      setSnapshotBaseline(0);
      setSnapshotError(null);
      setIsRestoring(false);
      setBidAckRound(null);
      setBidErrorRaw(null);
      setActionError(null);
      hasConnectedRef.current = false;
      trickWonCountRef.current = 0;
      completedTableRef.current = null;
      if (revealTimeoutRef.current) clearTimeout(revealTimeoutRef.current);
      if (revealIntervalRef.current) clearInterval(revealIntervalRef.current);
      revealTimeoutRef.current = null;
      revealIntervalRef.current = null;
      setRevealedTrick(null);
      setRevealSecondsLeft(0);
    });
  }, [lobbyId, playerId]);

  useEffect(() => {
    if (playerId === null || playerId === undefined) return;
    queueMicrotask(() => {
      void refreshSnapshot("mount");
    });
  }, [refreshSnapshot, playerId]);

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

  useEffect(() => {
    let timer: ReturnType<typeof setTimeout> | undefined;
    const schedule = (reason: string) => {
      if (timer) clearTimeout(timer);
      timer = setTimeout(() => void refreshSnapshot(reason), 300);
    };
    const onForeground = () => {
      if (document.visibilityState === "visible") schedule("foreground");
    };
    const onOnline = () => schedule("online");
    document.addEventListener("visibilitychange", onForeground);
    window.addEventListener("online", onOnline);
    window.addEventListener("focus", onOnline);
    return () => {
      if (timer) clearTimeout(timer);
      document.removeEventListener("visibilitychange", onForeground);
      window.removeEventListener("online", onOnline);
      window.removeEventListener("focus", onOnline);
    };
  }, [refreshSnapshot]);

  const eventBasedState = useMemo<GameBoardState>(() => {
    return gameEvents.reduce(
      (state, event) => gameReducer(state, event, reducerPlayerId),
      initialGameBoardState
    );
  }, [gameEvents, reducerPlayerId]);

  const gameState = useMemo<GameBoardState>(() => {
    if (!snapshotState) return eventBasedState;
    const tail = snapshotBaseline <= gameEvents.length
      ? gameEvents.slice(snapshotBaseline)
      : gameEvents;
    return tail.reduce((state, event) => gameReducer(state, event, reducerPlayerId), snapshotState);
  }, [snapshotState, snapshotBaseline, gameEvents, eventBasedState, reducerPlayerId]);

  useEffect(() => {
    if (gameState.table.length > 0) {
      completedTableRef.current = {
        entries: gameState.table,
        winningCard: gameState.winningCard,
      };
    }
  }, [gameState.table, gameState.winningCard]);

  const clearRevealTimers = () => {
    if (revealTimeoutRef.current) clearTimeout(revealTimeoutRef.current);
    if (revealIntervalRef.current) clearInterval(revealIntervalRef.current);
    revealTimeoutRef.current = null;
    revealIntervalRef.current = null;
  };

  useEffect(() => {
    if (trickWonCount <= trickWonCountRef.current) {
      if (trickWonCount < trickWonCountRef.current) {
        trickWonCountRef.current = trickWonCount;
        clearRevealTimers();
        setRevealedTrick(null);
        setRevealSecondsLeft(0);
      }
      return;
    }
    trickWonCountRef.current = trickWonCount;
    const snapshot = completedTableRef.current;
    const winnerId = gameState.lastTrick?.winnerId;
    if (!snapshot || snapshot.entries.length === 0 || winnerId === undefined) return;
    clearRevealTimers();
    setRevealedTrick({
      entries: snapshot.entries,
      winningCard: snapshot.winningCard,
      winnerId,
    });
    const deadline = Date.now() + TRICK_REVEAL_SECONDS * 1000;
    setRevealSecondsLeft(TRICK_REVEAL_SECONDS);
    revealIntervalRef.current = setInterval(() => {
      setRevealSecondsLeft(Math.max(0, Math.ceil((deadline - Date.now()) / 1000)));
    }, 200);
    revealTimeoutRef.current = setTimeout(() => {
      clearRevealTimers();
      setRevealedTrick(null);
      setRevealSecondsLeft(0);
    }, TRICK_REVEAL_SECONDS * 1000);
  }, [trickWonCount]);

  useEffect(
    () => () => {
      if (revealTimeoutRef.current) clearTimeout(revealTimeoutRef.current);
      if (revealIntervalRef.current) clearInterval(revealIntervalRef.current);
    },
    [],
  );

  const isMyTurn = gameState.currentTurn.isMyTurn && playerId !== null;
  const hasAlreadyBid = playerId !== null && gameState.bids[playerId] !== undefined;
  const bidAckedThisRound = bidAckRound === gameState.round;
  const canChooseTrump =
    isMyTurn &&
    gameState.currentTurn.actionType === "CHOOSE_TRUMP" &&
    gameState.status === "CHOOSING_TRUMP";
  const canBid =
    isMyTurn &&
    gameState.currentTurn.actionType === "BID" &&
    gameState.status === "BIDDING" &&
    !hasAlreadyBid &&
    !bidAckedThisRound;
  const canPlay =
    isMyTurn &&
    gameState.currentTurn.actionType === "PLAY_CARD" &&
    gameState.status === "PLAYING";

  const bidsTotal = useMemo(
    () => Object.values(gameState.bids).reduce((sum, bid) => sum + Number(bid ?? 0), 0),
    [gameState.bids]
  );

  const forbiddenBid = useMemo<number | null>(() => {
    if (playerId === null) return null;
    if (gameState.invalidBid !== null && gameState.invalidBid !== undefined) {
      return gameState.invalidBid;
    }
    const totalPlayers = lobby?.players.length ?? 0;
    if (totalPlayers <= 0) return null;
    if (gameState.status !== "BIDDING") return null;
    if (gameState.bids[playerId] !== undefined) return null;
    if (Object.keys(gameState.bids).length !== totalPlayers - 1) return null;
    const forbidden = gameState.round - bidsTotal;
    if (!Number.isInteger(forbidden) || forbidden < 0 || forbidden > gameState.round) {
      return null;
    }
    return forbidden;
  }, [gameState.invalidBid, gameState.status, gameState.bids, gameState.round, lobby?.players.length, bidsTotal, playerId]);

  const bidWarning = useMemo<string | null>(() => {
    if (!canBid || forbiddenBid === null || forbiddenBid === undefined) return null;
    return (
      `La puntata ${forbiddenBid} non è valida: con ${bidsTotal} già puntati, ` +
      `la somma (${bidsTotal} + ${forbiddenBid}) sarebbe uguale al numero di carte del Round ${gameState.round}.`
    );
  }, [canBid, forbiddenBid, bidsTotal, gameState.round]);

  const bidError = useMemo<string | null>(() => {
    if (!bidErrorRaw) return null;
    if (playerId === null) return null;
    if (bidErrorRaw.round !== gameState.round) return null;
    if (gameState.bids[playerId] !== undefined) return null;
    return bidErrorRaw.message;
  }, [bidErrorRaw, gameState.round, gameState.bids, playerId]);

  const setBidError = useCallback(
    (message: string | null) => {
      if (message === null) {
        setBidErrorRaw(null);
      } else {
        setBidErrorRaw({ round: gameState.round, message });
      }
    },
    [gameState.round]
  );

  const lobbyWarning = useMemo<string | null>(() => {
    if (lobby?.status === "PAUSED") {
      return "Partita in pausa: stai tornando alla lobby, premi Riprendi Partita per continuare.";
    }
    if (lobby?.status === "DISCONNECTING") {
      return "Un giocatore si è disconnesso: se è il tuo turno puoi comunque giocare, altrimenti attendi la riconnessione.";
    }
    return null;
  }, [lobby?.status]);

  const playersMap = useMemo(() => {
    return buildPlayersMap(lobby?.players ?? [], connectedPlayerIds);
  }, [lobby?.players, connectedPlayerIds]);

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

  const handleChooseTrump = useCallback(
    async (color?: CardColor) => {
      const colorToChoose = color ?? selectedColor;
      try {
        setIsSubmitting(true);
        setActionStatus(`Choosing trump color ${colorToChoose}...`);
        await chooseTrumpColor(lobbyId, colorToChoose);
        setActionError(null);
        setActionStatus(`Trump color chosen: ${colorToChoose}`);
      } catch (error) {
        const code = extractApiErrorCode(error);
        const msg = error instanceof Error ? error.message : String(error);
        if (code) {
          setActionError(formatGameActionError(code, gameState.round));
          setActionStatus(`Briscola rifiutata: ${shortGameActionReason(code)}.`);
        } else {
          setActionStatus(`Error choosing trump: ${msg}`);
        }
      } finally {
        setIsSubmitting(false);
      }
    },
    [lobbyId, selectedColor, gameState.round]
  );

  const handlePlaceBid = useCallback(
    async (bid?: number) => {
      if (playerId === null) return;
      const bidToPlace = bid !== undefined ? bid : bidInput;
      if (gameState.status !== "BIDDING" || !isMyTurn) {
        setActionStatus("Non è il tuo turno per puntare.");
        return;
      }
      if (gameState.bids[playerId] !== undefined || bidAckRound === gameState.round) {
        setActionStatus(`Hai già puntato per il Round ${gameState.round}.`);
        return;
      }
      if (forbiddenBid !== null && forbiddenBid !== undefined && bidToPlace === forbiddenBid) {
        const friendly = formatInvalidBidError(gameState.round, bidToPlace);
        setBidError(friendly);
        setActionStatus(`Puntata ${bidToPlace} rifiutata: non valida per il Round ${gameState.round}.`);
        return;
      }
      try {
        setIsSubmitting(true);
        setActionStatus(`Placing bid ${bidToPlace}...`);
        await placeBid(lobbyId, bidToPlace);
        setBidAckRound(gameState.round);
        setBidError(null);
        setActionError(null);
        setActionStatus(`Bid placed successfully: ${bidToPlace}`);
      } catch (error) {
        const code = extractApiErrorCode(error);
        const msg = error instanceof Error ? error.message : String(error);
        const isInvalidBid =
          code.includes("InvalidBid") || msg.includes("InvalidBid");
        if (isInvalidBid) {
          const parsed = parseInvalidBid(`${code} ${msg}`);
          const round = parsed ? parsed.round : gameState.round;
          const invalid = parsed ? parsed.bid : bidToPlace;
          const friendly = formatInvalidBidError(round, invalid);
          setBidError(friendly);
          setActionStatus(`Puntata ${invalid} rifiutata: non valida per il Round ${round}.`);
        } else if (code) {
          const friendly = formatGameActionError(code, gameState.round, bidToPlace);
          setBidError(friendly);
          setActionStatus(`Puntata ${bidToPlace} rifiutata: ${shortGameActionReason(code)}.`);
        } else {
          setActionStatus(`Error placing bid: ${msg}`);
        }
      } finally {
        setIsSubmitting(false);
      }
    },
    [bidAckRound, bidInput, forbiddenBid, gameState.bids, gameState.round, gameState.status, isMyTurn, lobbyId, playerId, setBidError]
  );

  const handlePlayCard = useCallback(
    async (card?: Card) => {
      const cardToPlay = card ?? selectedCard;
      if (!cardToPlay) {
        setActionStatus("Seleziona una carta dalla mano prima di giocare.");
        return;
      }

      try {
        setIsSubmitting(true);
        setActionStatus("Playing card...");
        await playCard(lobbyId, cardToPlay);
        setActionError(null);
        setActionStatus("Card played successfully");
        setSelectedCard(null);
      } catch (error) {
        const code = extractApiErrorCode(error);
        const msg = error instanceof Error ? error.message : String(error);
        if (code) {
          setActionError(formatGameActionError(code, gameState.round));
          setActionStatus(`Carta rifiutata: ${shortGameActionReason(code)}.`);
        } else {
          setActionStatus(`Error playing card: ${msg}`);
        }
      } finally {
        setIsSubmitting(false);
      }
    },
    [gameState.round, lobbyId, selectedCard]
  );

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
    // Reveal di fine presa (tavolo congelato + conto alla rovescia)
    revealedTrick,
    revealSecondsLeft,
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
    // Bidding: puntata vietata (somma != round) e messaggi per il banner
    forbiddenBid,
    bidsTotal,
    bidWarning,
    bidError,
    setBidError,
    // Errori di azione (es. carta rifiutata con GamePaused) e stato lobby
    actionError,
    setActionError,
    lobbyWarning,
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
