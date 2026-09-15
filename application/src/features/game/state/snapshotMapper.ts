"use client";

import { getTrumpColor } from "./gameReducer";
import type {
  Card,
  CardColor,
  GameBoardState,
  PlayedCardEntry,
  Scoreboard,
  Trump,
} from "../types";

/**
 * Raw shape of `GET /api/lobby/{lobbyId}/game` (backend `PlayerGameState`).
 * Circe encodes the sealed trait as a single-key object naming the variant,
 * e.g. `{ "Playing": { "core": {...}, "bids": {...}, ... } }`.
 */
export interface SnapshotCore {
  playersIds: number[];
  hand: Card[];
  trump: Trump;
  round: number;
  dealerId: number;
  scoreboard: Scoreboard;
}

export interface SnapshotTable {
  playedCards: PlayedCardEntry[];
  followingColor?: CardColor | null;
}

export type PlayerGameSnapshot =
  | { ChoosingTrump: { core: SnapshotCore } }
  | { Bidding: { core: SnapshotCore; bids: Record<string, number>; playerTurn: number } }
  | {
      Playing: {
        core: SnapshotCore;
        bids: Record<string, number>;
        table: SnapshotTable;
        playerTurn: number;
        tricksWon: Record<string, number>;
        currentWinner?: number | null;
      };
    }
  | { Ended: { playersIds: number[]; scoreboard: Scoreboard } };

function toNumericRecord(input: unknown): Record<number, number> {
  if (!input || typeof input !== "object") return {};
  const out: Record<number, number> = {};
  for (const [key, value] of Object.entries(input as Record<string, unknown>)) {
    const id = Number(key);
    const amount = Number(value);
    if (!Number.isNaN(id) && !Number.isNaN(amount)) out[id] = amount;
  }
  return out;
}

function normalizeScoreboard(input: unknown): Scoreboard | null {
  if (!input || typeof input !== "object") return null;
  const entries = Object.entries(input as Record<string, unknown>);
  if (entries.length === 0) return null;
  return input as Scoreboard;
}

function asCardList(input: unknown): Card[] {
  return Array.isArray(input) ? (input as Card[]) : [];
}

function asTableEntries(input: unknown): PlayedCardEntry[] {
  if (!Array.isArray(input)) return [];
  return (input as PlayedCardEntry[]).filter(
    (entry) =>
      entry !== null &&
      typeof entry === "object" &&
      typeof (entry as { playerId?: unknown }).playerId === "number" &&
      (entry as { card?: unknown }).card !== undefined
  );
}

/**
 * Replica della regola backend `Hand.legalCards(table)`: le speciali
 * (Wizard/Jester) sono sempre giocabili, le standard devono seguire il
 * `followingColor` quando il giocatore ha quel seme in mano.
 * Serve a ripristinare subito l'evidenziazione delle carte giocabili dopo
 * un reload, visto che lo snapshot non include `legalCards` (arrivano solo
 * via evento `WaitingForCard`).
 */
export function computeLegalCards(hand: Card[], followingColor: CardColor | null): Card[] {
  if (!followingColor) return [...hand];
  const hasFollowingColor = hand.some(
    (card) => card.type === "Standard" && card.color === followingColor
  );
  if (!hasFollowingColor) return [...hand];
  return hand.filter(
    (card) => card.type !== "Standard" || card.color === followingColor
  );
}

function baseStateFromCore(core: SnapshotCore): Omit<
  GameBoardState,
  | "status"
  | "currentTurn"
  | "bids"
  | "tricksWon"
  | "table"
  | "winningCard"
  | "followingColor"
  | "invalidBid"
> {
  const trump = (core.trump ?? null) as Trump | null;
  return {
    round: Number(core.round ?? 1),
    trump,
    effectiveTrumpColor: getTrumpColor(trump),
    hand: asCardList(core.hand),
    legalCards: [],
    scoreboard: normalizeScoreboard(core.scoreboard),
    lastTrick: null,
    lastError: null,
    eventsHistory: [],
  };
}

/**
 * Converte uno snapshot backend nel `GameBoardState` usato dalla board.
 * Funzione pura e testabile: a parita' di snapshot e playerId produce
 * sempre lo stesso stato iniziale su cui poi vengono ridotti gli eventi
 * live ricevuti via WebSocket.
 */
export function mapSnapshotToBoardState(
  snapshot: PlayerGameSnapshot,
  myPlayerId: number
): GameBoardState {
  if ("ChoosingTrump" in snapshot) {
    const core = snapshot.ChoosingTrump.core;
    return {
      ...baseStateFromCore(core),
      status: "CHOOSING_TRUMP",
      bids: {},
      tricksWon: {},
      table: [],
      winningCard: null,
      followingColor: null,
      invalidBid: null,
      currentTurn: {
        actionType: "CHOOSE_TRUMP",
        playerId: Number(core.dealerId),
        isMyTurn: Number(core.dealerId) === myPlayerId,
      },
    };
  }

  if ("Bidding" in snapshot) {
    const { core, bids, playerTurn } = snapshot.Bidding;
    const turn = Number(playerTurn);
    return {
      ...baseStateFromCore(core),
      status: "BIDDING",
      bids: toNumericRecord(bids),
      tricksWon: {},
      table: [],
      winningCard: null,
      followingColor: null,
      invalidBid: null,
      currentTurn: {
        actionType: "BID",
        playerId: turn,
        isMyTurn: turn === myPlayerId,
      },
    };
  }

  if ("Playing" in snapshot) {
    const { core, bids, table, playerTurn, tricksWon, currentWinner } = snapshot.Playing;
    const turn = Number(playerTurn);
    const tableEntries = asTableEntries(table?.playedCards);
    const followingColor = (table?.followingColor ?? null) as CardColor | null;
    const winnerId = currentWinner === undefined || currentWinner === null ? null : Number(currentWinner);
    const winningCard =
      winnerId !== null
        ? (tableEntries.find((entry) => entry.playerId === winnerId)?.card ?? null)
        : null;
    const hand = asCardList(core.hand);
    return {
      ...baseStateFromCore(core),
      status: "PLAYING",
      bids: toNumericRecord(bids),
      tricksWon: toNumericRecord(tricksWon),
      table: tableEntries,
      winningCard,
      followingColor,
      invalidBid: null,
      legalCards: turn === myPlayerId ? computeLegalCards(hand, followingColor) : [],
      currentTurn: {
        actionType: "PLAY_CARD",
        playerId: turn,
        isMyTurn: turn === myPlayerId,
      },
    };
  }

  const { scoreboard } = snapshot.Ended;
  const normalized = normalizeScoreboard(scoreboard);
  let lastRound = 1;
  if (normalized) {
    for (const entries of Object.values(normalized)) {
      for (const entry of entries) {
        if (typeof entry?.round === "number" && entry.round > lastRound) {
          lastRound = entry.round;
        }
      }
    }
  }
  return {
    status: "GAME_ENDED",
    round: lastRound,
    trump: null,
    effectiveTrumpColor: null,
    hand: [],
    legalCards: [],
    table: [],
    winningCard: null,
    followingColor: null,
    bids: {},
    tricksWon: {},
    scoreboard: normalized,
    currentTurn: { actionType: "NONE", playerId: null, isMyTurn: false },
    lastTrick: null,
    lastError: null,
    invalidBid: null,
    eventsHistory: [],
  };
}
