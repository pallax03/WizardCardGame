import type {
  Card,
  CardColor,
  EventMessage,
  GameBoardState,
  Scoreboard,
  Trump,
} from "../types";

/**
 * Checks whether two cards are identical in type, color, and rank/id.
 */
export function cardEquals(
  a: Card | null | undefined,
  b: Card | null | undefined
): boolean {
  if (!a || !b) return a === b;
  if (a.type !== b.type) return false;
  if (a.type === "Standard" && b.type === "Standard") {
    return a.color === b.color && a.rank === b.rank;
  }
  if (a.type === "Wizard" && b.type === "Wizard") {
    return a.id === b.id;
  }
  if (a.type === "Jester" && b.type === "Jester") {
    return a.id === b.id;
  }
  return false;
}

/**
 * Formats a card into a human-readable string (e.g. "Red 7", "Wizard #1", "Jester #2").
 */
export function cardToString(card: Card): string {
  if (card.type === "Standard") {
    return `${card.color} ${card.rank}`;
  }
  if (card.type === "Wizard") {
    return `Wizard #${card.id}`;
  }
  if (card.type === "Jester") {
    return `Jester #${card.id}`;
  }
  return "Unknown Card";
}

/**
 * Checks if a given card is present in a list of cards (e.g. legal cards).
 */
export function isCardInList(target: Card, list: Card[]): boolean {
  return list.some((c) => cardEquals(c, target));
}

/**
 * Extracts the effective trump color from a Trump object, if resolved.
 */
export function getTrumpColor(trump: Trump | null): CardColor | null {
  if (!trump) return null;
  if (trump.type === "Standard" || trump.type === "WizardResolved") {
    return trump.color;
  }
  return null;
}

export const initialGameBoardState: GameBoardState = {
  status: "WAITING",
  round: 1,
  trump: null,
  effectiveTrumpColor: null,
  hand: [],
  legalCards: [],
  table: [],
  winningCard: null,
  followingColor: null,
  bids: {},
  tricksWon: {},
  scoreboard: null,
  currentTurn: {
    actionType: "NONE",
    playerId: null,
    isMyTurn: false,
  },
  lastTrick: null,
  lastError: null,
  eventsHistory: [],
};

/**
 * Pure reducer function that advances GameBoardState based on an incoming EventMessage.
 *
 * Designed to be lean, predictable, and fully testable:
 * Given a history of EventMessages and myPlayerId, it constructs the current game state.
 */
export function gameReducer(
  state: GameBoardState,
  eventMessage: EventMessage,
  myPlayerId: number
): GameBoardState {
  const { event } = eventMessage;
  const action = event.action;
  const fields = (event.fields ?? {}) as Record<string, unknown>;
  const eventPlayerId = (event.playerId ?? fields.playerId) as number | undefined;

  // Add event to history
  const updatedHistory = [...state.eventsHistory, eventMessage];

  switch (action) {
    case "GameStarted": {
      return {
        ...initialGameBoardState,
        status: "WAITING",
        eventsHistory: updatedHistory,
      };
    }

    case "RoundStarted": {
      const newRound = Number(fields.round ?? state.round);
      return {
        ...state,
        round: newRound,
        table: [],
        winningCard: null,
        followingColor: null,
        bids: {},
        tricksWon: {},
        lastTrick: null,
        lastError: null,
        currentTurn: { actionType: "NONE", playerId: null, isMyTurn: false },
        eventsHistory: updatedHistory,
      };
    }

    case "CardsDealt": {
      // CardsDealt is DestinationScoped (private to destinationId)
      const targetDestinationId = event.destinationId ?? fields.playerId;
      const isForMe = targetDestinationId === myPlayerId;

      if (!isForMe) {
        return { ...state, eventsHistory: updatedHistory };
      }

      const dealtTrump = (fields.trump ?? null) as Trump | null;
      return {
        ...state,
        round: Number(fields.round ?? state.round),
        hand: (fields.hand as Card[]) ?? [],
        legalCards: [],
        trump: dealtTrump,
        effectiveTrumpColor: getTrumpColor(dealtTrump),
        table: [],
        winningCard: null,
        followingColor: null,
        bids: {},
        tricksWon: {},
        lastError: null,
        eventsHistory: updatedHistory,
      };
    }

    case "WaitingForTrump": {
      const dealerId = Number(eventPlayerId);
      return {
        ...state,
        status: "CHOOSING_TRUMP",
        currentTurn: {
          actionType: "CHOOSE_TRUMP",
          playerId: dealerId,
          isMyTurn: dealerId === myPlayerId,
        },
        eventsHistory: updatedHistory,
      };
    }

    case "TrumpColorResolved": {
      const chosenColor = fields.color as CardColor;
      return {
        ...state,
        effectiveTrumpColor: chosenColor,
        eventsHistory: updatedHistory,
      };
    }

    case "PhaseChanged": {
      const phaseName = String(fields.phaseName ?? "");
      let newStatus = state.status;
      if (phaseName === "ChoosingTrump") newStatus = "CHOOSING_TRUMP";
      else if (phaseName === "Bidding") newStatus = "BIDDING";
      else if (phaseName === "Playing") newStatus = "PLAYING";

      return {
        ...state,
        status: newStatus,
        eventsHistory: updatedHistory,
      };
    }

    case "WaitingForBid": {
      const bidderId = Number(eventPlayerId);
      return {
        ...state,
        status: "BIDDING",
        currentTurn: {
          actionType: "BID",
          playerId: bidderId,
          isMyTurn: bidderId === myPlayerId,
        },
        eventsHistory: updatedHistory,
      };
    }

    case "BidPlaced": {
      const bidderId = Number(eventPlayerId);
      const bidAmount = Number(fields.bid ?? 0);
      return {
        ...state,
        bids: {
          ...state.bids,
          [bidderId]: bidAmount,
        },
        lastError: null,
        eventsHistory: updatedHistory,
      };
    }

    case "WaitingForCard": {
      const activePlayerId = Number(eventPlayerId);
      const isMyTurn = activePlayerId === myPlayerId;
      const legalCards = isMyTurn ? ((fields.legalCards as Card[]) ?? []) : state.legalCards;

      return {
        ...state,
        status: "PLAYING",
        currentTurn: {
          actionType: "PLAY_CARD",
          playerId: activePlayerId,
          isMyTurn,
        },
        legalCards: isMyTurn ? legalCards : state.legalCards,
        eventsHistory: updatedHistory,
      };
    }

    case "CardPlayed": {
      const cardPlayerId = Number(eventPlayerId);
      const playedCard = fields.card as Card;
      const winningCard = (fields.winningCard as Card | undefined) ?? null;
      const followingColor = (fields.followingColor as CardColor | undefined) ?? null;

      // Add to table
      const newTable = [...state.table, { playerId: cardPlayerId, card: playedCard }];

      // If it was my card, remove it from my hand and clear legalCards
      let newHand = state.hand;
      let newLegal = state.legalCards;
      if (cardPlayerId === myPlayerId) {
        newHand = state.hand.filter((c) => !cardEquals(c, playedCard));
        newLegal = [];
      }

      return {
        ...state,
        table: newTable,
        winningCard,
        followingColor,
        hand: newHand,
        legalCards: newLegal,
        lastError: null,
        eventsHistory: updatedHistory,
      };
    }

    case "TrickWon": {
      const winnerId = Number(fields.winnerId ?? eventPlayerId);
      const tricksCount = Number(fields.tricksWon ?? 0);
      const trickedCards = (fields.trickedCards as Card[]) ?? [];

      return {
        ...state,
        tricksWon: {
          ...state.tricksWon,
          [winnerId]: tricksCount,
        },
        lastTrick: {
          winnerId,
          cards: trickedCards,
          tricksWon: tricksCount,
        },
        table: [],
        winningCard: null,
        followingColor: null,
        eventsHistory: updatedHistory,
      };
    }

    case "RoundScored": {
      const scoreboard = (fields.scoreboard ?? {}) as Scoreboard;
      return {
        ...state,
        status: "ROUND_SCORED",
        scoreboard,
        currentTurn: {
          actionType: "NONE",
          playerId: null,
          isMyTurn: false,
        },
        eventsHistory: updatedHistory,
      };
    }

    case "GameEnded": {
      const finalScores = (fields.finalScores ?? {}) as Scoreboard;
      return {
        ...state,
        status: "GAME_ENDED",
        scoreboard: finalScores,
        currentTurn: {
          actionType: "NONE",
          playerId: null,
          isMyTurn: false,
        },
        eventsHistory: updatedHistory,
      };
    }

    case "ActionFailed": {
      const targetDest = event.destinationId ?? fields.playerId;
      const isForMe = targetDest === myPlayerId;
      const reasonObj = fields.reason as { error?: string } | undefined;
      const errorMsg = reasonObj?.error ?? "Invalid action attempted";

      return {
        ...state,
        lastError: isForMe ? `Action Failed: ${errorMsg}` : state.lastError,
        eventsHistory: updatedHistory,
      };
    }

    case "GameAborted": {
      const reason = String(fields.reason ?? "Game aborted by server");
      return {
        ...state,
        status: "ABORTED",
        lastError: `Game aborted: ${reason}`,
        currentTurn: {
          actionType: "NONE",
          playerId: null,
          isMyTurn: false,
        },
        eventsHistory: updatedHistory,
      };
    }

    default: {
      return {
        ...state,
        eventsHistory: updatedHistory,
      };
    }
  }
}
