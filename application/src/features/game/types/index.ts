import type { EventMessage } from "@/features/chat/types";

// --- Card Types ---
export type CardColor = "Red" | "Yellow" | "Green" | "Blue";

export type CardRank = number; // 1 to 13

export interface StandardCard {
  type: "Standard";
  color: CardColor;
  rank: CardRank;
}

export interface WizardCard {
  type: "Wizard";
  id: number;
}

export interface JesterCard {
  type: "Jester";
  id: number;
}

export type SpecialCard = WizardCard | JesterCard;

export type Card = StandardCard | SpecialCard;

// --- Trump Types ---
export type Trump =
  | { type: "Standard"; card: Card; color: CardColor }
  | { type: "WizardResolved"; card: Card; color: CardColor }
  | { type: "WizardUnresolved"; card: Card }
  | { type: "Jester"; card: Card }
  | { type: "Absent" };

// --- Scoreboard Types ---
export interface ScoreEntry {
  round: number;
  score: number;
  bid: number;
}

// Map from PlayerId (as string key) to list of score entries for each round
export type Scoreboard = Record<string, ScoreEntry[]>;

// --- Game Phases ---
export type GamePhase =
  | "WAITING"
  | "CHOOSING_TRUMP"
  | "BIDDING"
  | "PLAYING"
  | "ROUND_SCORED"
  | "GAME_ENDED"
  | "ABORTED";

// --- Table / Trick ---
export interface PlayedCardEntry {
  playerId: number;
  card: Card;
}

// --- Player Turn Info ---
export type TurnActionType = "NONE" | "CHOOSE_TRUMP" | "BID" | "PLAY_CARD";

export interface PlayerTurnInfo {
  actionType: TurnActionType;
  playerId: number | null;
  isMyTurn: boolean;
}

// --- Specific Game Event Payloads (mapped from backend WizardEvent codecs) ---

export interface GameStartedPayload {
  playersIds: number[];
}

export interface CardsDealtPayload {
  playerId: number;
  hand: Card[];
  trump: Trump;
  round: number;
}

export interface WaitingForTrumpPayload {
  playerId: number;
}

export interface TrumpColorResolvedPayload {
  playerId: number;
  color: CardColor;
}

export interface RoundStartedPayload {
  round: number;
}

export interface WaitingForBidPayload {
  playerId: number;
  round: number;
}

export interface BidPlacedPayload {
  playerId: number;
  bid: number;
}

export interface WaitingForCardPayload {
  playerId: number;
  legalCards: Card[];
}

export interface CardPlayedPayload {
  playerId: number;
  card: Card;
  winningCard?: Card | null;
  followingColor?: CardColor | null;
}

export interface TrickWonPayload {
  winnerId: number;
  tricksWon: number;
  trickedCards: Card[];
}

export interface RoundScoredPayload {
  playersIds: number[];
  scoreboard: Scoreboard;
}

export interface PhaseChangedPayload {
  phaseName: string;
}

export interface GameEndedPayload {
  playersIds: number[];
  finalScores: Scoreboard;
}

export interface GameAbortedPayload {
  reason: string;
}

export interface ActionFailedPayload {
  playerId: number;
  reason: {
    error: string;
  };
}

// Re-export EventMessage for convenience
export type { EventMessage };

// --- Comprehensive GameBoard State ---
export interface GameBoardState {
  status: GamePhase;
  round: number;
  trump: Trump | null;
  effectiveTrumpColor: CardColor | null;
  hand: Card[];
  legalCards: Card[];
  table: PlayedCardEntry[];
  winningCard: Card | null;
  followingColor: CardColor | null;
  bids: Record<number, number>; // playerId -> bid
  tricksWon: Record<number, number>; // playerId -> tricks
  scoreboard: Scoreboard | null;
  currentTurn: PlayerTurnInfo;
  lastTrick: {
    winnerId: number;
    cards: Card[];
    tricksWon: number;
  } | null;
  lastError: string | null;
  eventsHistory: EventMessage[];
}
