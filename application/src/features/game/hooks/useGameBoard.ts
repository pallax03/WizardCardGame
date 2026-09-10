"use client";

import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { useLobbySession } from "@/features/lobby-session";
import type { EventMessage } from "@/features/chat/types";
import { chooseTrumpColor, getPlayerGameSnapshot, placeBid, playCard } from "../api";
import {
  formatInvalidBidError,
  gameReducer,
  initialGameBoardState,
  isCardInList,
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
  const playerId = resolvedPlayerId ?? 1;

  // Local selection states for user interaction
  const [selectedCard, setSelectedCard] = useState<Card | null>(null);
  const [bidInput, setBidInput] = useState<number>(0);
  const [selectedColor, setSelectedColor] = useState<CardColor>("Red");
  const [isSubmitting, setIsSubmitting] = useState<boolean>(false);
  const [actionStatus, setActionStatus] = useState<string | null>(null);
  // Errore locale di bid non valida: finisce nel GameTurnBanner come lastError.
  // Serve perché il backend risponde 400 via HTTP (senza evento WS) quando
  // la somma delle puntate pareggerebbe il numero di carte del round.
  // Memorizziamo anche il round: l'errore è valido solo per quel round e
  // finché il player non ha puntato (niente useEffect di reset).
  const [bidErrorRaw, setBidErrorRaw] = useState<{ round: number; message: string } | null>(null);
  // Ack ottimistico: dopo una bid andata a buon fine nascondiamo subito i
  // controlli, senza aspettare l'echo WS (BidPlaced/TurnOf).
  const [bidAckRound, setBidAckRound] = useState<number | null>(null);

  // Snapshot ripristinato dal backend (GET /api/lobby/{lobbyId}/game).
  // Serve da base quando la cronologia WS e' andata persa (reload/chiusura
  // finestra). Gli eventi arrivati dopo il fetch vengono ridotti sopra.
  const [snapshotState, setSnapshotState] = useState<GameBoardState | null>(null);
  const [snapshotBaseline, setSnapshotBaseline] = useState(0);
  const [isRestoring, setIsRestoring] = useState(false);
  const [snapshotError, setSnapshotError] = useState<string | null>(null);

  // Reveal di fine presa: quando arriva TrickWon il tavolo live si svuota
  // subito, ma qui congeliamo le carte giocate e le mostriamo ancora per
  // qualche secondo con conto alla rovescia.
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

  // Filter all EventMessage received from WebSocket
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
      setBidAckRound(null);
      setBidErrorRaw(null);
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

  // Congela l'ultimo tavolo completo: finche' ci sono carte sul tavolo live,
  // questa e' la foto che mostreremo quando arrivera' il TrickWon.
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

  // Nuova presa vinta -> mostra il tavolo congelato con conto alla rovescia.
  // Si reagisce solo al conteggio dei TrickWon: lo stato live sotto continua
  // ad avanzare normalmente (prossima mano) mentre l'overlay e' visibile.
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
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [trickWonCount]);

  // Cleanup timer allo smontaggio.
  useEffect(
    () => () => {
      if (revealTimeoutRef.current) clearTimeout(revealTimeoutRef.current);
      if (revealIntervalRef.current) clearInterval(revealIntervalRef.current);
    },
    [],
  );

  // Derived helpers
  // NB: i WaitingFor* sono privati (solo al destinatario), quindi isMyTurn si
  // basa anche sui TurnOf broadcast (cfr. gameReducer). In più richiediamo
  // fase coerente e, per le bid, che non si sia già puntato in questo round.
  const isMyTurn = gameState.currentTurn.isMyTurn;
  const hasAlreadyBid = gameState.bids[playerId] !== undefined;
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

  // Somma delle bid piazzate finora nel round corrente.
  const bidsTotal = useMemo(
    () => Object.values(gameState.bids).reduce((sum, bid) => sum + Number(bid ?? 0), 0),
    [gameState.bids]
  );

  // Puntata vietata per il bidder corrente (regola Wizard: somma != round).
  // Fonte primaria: `WaitingForBid.invalidBid` del backend; fallback locale
  // quando sono l'ultimo bidder (round - somma delle altre bid).
  const forbiddenBid = useMemo<number | null>(() => {
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

  // Avviso preventivo mostrato nel GameTurnBanner quando tocca a me puntare
  // e una puntata è vietata (es. somma pareggerebbe le carte del round).
  const bidWarning = useMemo<string | null>(() => {
    if (!canBid || forbiddenBid === null || forbiddenBid === undefined) return null;
    return (
      `La puntata ${forbiddenBid} non è valida: con ${bidsTotal} già puntati, ` +
      `la somma (${bidsTotal} + ${forbiddenBid}) sarebbe uguale al numero di carte del Round ${gameState.round}.`
    );
  }, [canBid, forbiddenBid, bidsTotal, gameState.round]);

  // Errore visibile solo se riferito al round corrente e se non ho ancora puntato.
  // Così non servono useEffect di reset (evita cascading renders).
  const bidError = useMemo<string | null>(() => {
    if (!bidErrorRaw) return null;
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
      // Doppia protezione lato client: una sola bid per round, solo in fase
      // di bidding e solo se è il mio turno.
      if (gameState.status !== "BIDDING" || !isMyTurn) {
        setActionStatus("Non è il tuo turno per puntare.");
        return;
      }
      if (gameState.bids[playerId] !== undefined || bidAckRound === gameState.round) {
        setActionStatus(`Hai già puntato per il Round ${gameState.round}.`);
        return;
      }
      // Validazione client della regola Wizard: l'ultimo bidder non può far
      // pareggiare somma e round. L'errore finisce nel GameTurnBanner.
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
        // Nascondi subito i controlli, senza aspettare l'echo WS.
        setBidAckRound(gameState.round);
        setBidError(null);
        setActionStatus(`Bid placed successfully: ${bidToPlace}`);
      } catch (error) {
        const code =
          typeof (error as { code?: unknown })?.code === "string"
            ? ((error as { code: string }).code)
            : "";
        const msg = error instanceof Error ? error.message : String(error);
        const isInvalidBid =
          code.includes("InvalidBid") || msg.includes("InvalidBid");
        if (isInvalidBid) {
          // Il backend codifica GameError.InvalidBid come "InvalidBid(round,bid)".
          const match = /InvalidBid\(\s*(\d+)\s*,\s*(-?\d+)\s*\)/.exec(`${code} ${msg}`);
          const round = match ? Number(match[1]) : gameState.round;
          const invalid = match ? Number(match[2]) : bidToPlace;
          const friendly = formatInvalidBidError(round, invalid);
          setBidError(friendly);
          setActionStatus(`Puntata ${invalid} rifiutata: non valida per il Round ${round}.`);
        } else {
          setActionStatus(`Error placing bid: ${msg}`);
        }
      } finally {
        setIsSubmitting(false);
      }
    },
    [bidAckRound, bidInput, forbiddenBid, gameState.bids, gameState.round, gameState.status, isMyTurn, lobbyId, playerId, setBidError]
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
