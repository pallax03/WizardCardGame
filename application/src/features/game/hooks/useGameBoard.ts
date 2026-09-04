"use client";

import { useCallback, useMemo, useState } from "react";
import { useLobbySession } from "@/features/lobby-session";
import type { EventMessage } from "@/features/chat/types";
import { chooseTrumpColor, placeBid, playCard } from "../api";
import { gameReducer, initialGameBoardState, isCardInList } from "../state/gameReducer";
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

  const playerId = customPlayerId ?? sessionPlayerId ?? 1;

  // Local selection states for user interaction
  const [selectedCard, setSelectedCard] = useState<Card | null>(null);
  const [bidInput, setBidInput] = useState<number>(0);
  const [selectedColor, setSelectedColor] = useState<CardColor>("Red");
  const [isSubmitting, setIsSubmitting] = useState<boolean>(false);
  const [actionStatus, setActionStatus] = useState<string | null>(null);

  // Filter all EventMessage received from WebSocket
  const gameEvents = useMemo(
    () => messages.filter((m): m is EventMessage => m.type === "event"),
    [messages]
  );

  // Reduce all events sequentially to derive current GameBoardState
  const gameState = useMemo<GameBoardState>(() => {
    return gameEvents.reduce(
      (state, event) => gameReducer(state, event, playerId),
      initialGameBoardState
    );
  }, [gameEvents, playerId]);

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
        await chooseTrumpColor(lobbyId, playerId, colorToChoose);
        setActionStatus(`Trump color chosen: ${colorToChoose}`);
      } catch (error) {
        const msg = error instanceof Error ? error.message : String(error);
        setActionStatus(`Error choosing trump: ${msg}`);
      } finally {
        setIsSubmitting(false);
      }
    },
    [lobbyId, playerId, selectedColor]
  );

  // Action: Place Bid
  const handlePlaceBid = useCallback(
    async (bid?: number) => {
      const bidToPlace = bid !== undefined ? bid : bidInput;
      try {
        setIsSubmitting(true);
        setActionStatus(`Placing bid ${bidToPlace}...`);
        await placeBid(lobbyId, playerId, bidToPlace);
        setActionStatus(`Bid placed successfully: ${bidToPlace}`);
      } catch (error) {
        const msg = error instanceof Error ? error.message : String(error);
        setActionStatus(`Error placing bid: ${msg}`);
      } finally {
        setIsSubmitting(false);
      }
    },
    [bidInput, lobbyId, playerId]
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
        await playCard(lobbyId, playerId, cardToPlay);
        setActionStatus("Card played successfully");
        setSelectedCard(null);
      } catch (error) {
        const msg = error instanceof Error ? error.message : String(error);
        setActionStatus(`Error playing card: ${msg}`);
      } finally {
        setIsSubmitting(false);
      }
    },
    [gameState.hand, gameState.legalCards, lobbyId, playerId, selectedCard]
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
