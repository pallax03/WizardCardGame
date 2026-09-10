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
  invalidBid: null,
  eventsHistory: [],
};

/**
 * Messaggio mostrato nel GameTurnBanner quando una bid non è valida.
 * Esempio tipico: ultimo bidder non può far pareggiare somma e round.
 */
export function formatInvalidBidError(round: number, bid: number): string {
  return (
    `Puntata non valida: ${bid} non ammessa al Round ${round} — ` +
    `la somma delle puntate non può essere uguale al numero di carte (${round}).`
  );
}

function parseInvalidBidField(value: unknown): number | null {
  const amount = Number(value);
  if (value === null || value === undefined) return null;
  if (!Number.isInteger(amount)) return null;
  return amount;
}

/**
 * Traduce un errore HTTP del backend (`ApiError.code`, es. `GamePaused` o
 * `GameActionRejected(InvalidBid(2,1))`) in un messaggio italiano da mostrare
 * nel GameTurnBanner. Il backend codifica `LobbyError` come solo `{"code": ...}`
 * senza `message`, quindi il codice è l'unica informazione affidabile.
 */
export function formatGameActionError(
  code: string,
  round: number,
  bidHint?: number
): string {
  // GameActionRejected avvolge il GameError: estrai il contenuto interno.
  const rejectedMatch = /GameActionRejected\((.*)\)\s*$/.exec(code);
  const inner = rejectedMatch ? rejectedMatch[1] : code;

  if (inner.includes("GamePaused")) {
    return (
      "Partita in pausa: un giocatore è offline o la partita è stata messa in pausa. " +
      "Attendi la riconnessione oppure torna alla lobby e premi Riprendi Partita."
    );
  }
  if (inner.includes("InvalidBid")) {
    const match = /InvalidBid\(\s*(\d+)\s*,\s*(-?\d+)\s*\)/.exec(inner);
    const parsedRound = match ? Number(match[1]) : round;
    const parsedBid = match ? Number(match[2]) : bidHint;
    if (parsedBid !== undefined && !Number.isNaN(parsedBid)) {
      return formatInvalidBidError(parsedRound, parsedBid);
    }
    return (
      `Puntata non valida al Round ${parsedRound} — ` +
      `la somma delle puntate non può essere uguale al numero di carte (${parsedRound}).`
    );
  }
  if (inner.includes("CardNotAllowed") || inner.includes("MustFollowColor")) {
    return "Carta non consentita: devi seguire il seme di mano se hai una carta di quel seme.";
  }
  if (inner.includes("CardNotInHand")) {
    return "Carta non consentita: la carta non è più nella tua mano (stato già aggiornato?).";
  }
  if (inner.includes("NotYourTurn")) {
    return "Non è il tuo turno: attendi che tocchi a te.";
  }
  if (inner.includes("InvalidAction")) {
    return "Azione non valida in questa fase della partita.";
  }
  if (inner.includes("PlayersOffline")) {
    return "Impossibile avviare: alcuni giocatori sono offline.";
  }
  if (inner.includes("NotAuthenticated")) {
    return "Sessione non riconosciuta: rientra nella lobby dalla home.";
  }
  if (code && code !== "SERVER_ERROR") {
    return `Azione rifiutata dal server (${code}). Riprova tra poco.`;
  }
  return "Azione rifiutata dal server. Riprova tra poco.";
}

/** Estrae `ApiError.code` (es. `GamePaused`) da un errore di fetch. */
export function extractApiErrorCode(error: unknown): string {
  if (typeof error === "object" && error !== null && "code" in error) {
    const code = (error as { code?: unknown }).code;
    if (typeof code === "string") return code;
  }
  return "";
}

/** Motivo breve per `actionStatus` (il testo esteso va nell'errore del banner). */
export function shortGameActionReason(code: string): string {
  if (code.includes("GamePaused")) return "partita in pausa";
  if (code.includes("CardNotAllowed") || code.includes("MustFollowColor")) {
    return "carta non consentita";
  }
  if (code.includes("NotYourTurn")) return "non è il tuo turno";
  if (code.includes("InvalidAction")) return "azione non valida in questa fase";
  return code || "errore del server";
}

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
  const destinationPlayerId = (event.destinationId ?? fields.destinationId) as number | undefined;

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
        invalidBid: null,
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
        invalidBid: null,
        eventsHistory: updatedHistory,
      };
    }

    case "WaitingForTrump": {
      const dealerId = destinationPlayerId;
      if (dealerId === undefined) return { ...state, eventsHistory: updatedHistory };
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
      const actorId = Number(eventPlayerId);
      const isTurnHolder =
        !Number.isNaN(actorId) && state.currentTurn.playerId === actorId;
      return {
        ...state,
        effectiveTrumpColor: chosenColor,
        // Il turno del dichiarante e' finito: lo azzeriamo in attesa del
        // TurnOf broadcast per il prossimo bidder.
        currentTurn: isTurnHolder
          ? { actionType: "NONE", playerId: null, isMyTurn: false }
          : state.currentTurn,
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

    case "TurnOf": {
      // Evento broadcast (lo ricevono tutti): e' l'unico segnale affidabile
      // per sapere di chi e' il turno quando tocca a un altro giocatore/bot.
      // Gli inviti WaitingFor* sono privati (solo al destinatario).
      const activeId =
        typeof eventPlayerId === "number" && !Number.isNaN(Number(eventPlayerId))
          ? Number(eventPlayerId)
          : null;
      if (activeId === null) return { ...state, eventsHistory: updatedHistory };
      const requested = String(fields.actionRequested ?? "");
      let actionType: GameBoardState["currentTurn"]["actionType"] = "NONE";
      let status = state.status;
      if (requested === "PlaceBid") {
        actionType = "BID";
        status = "BIDDING";
      } else if (requested === "PlayCard") {
        actionType = "PLAY_CARD";
        status = "PLAYING";
      } else if (requested === "ResolveTrumpColor") {
        actionType = "CHOOSE_TRUMP";
        status = "CHOOSING_TRUMP";
      }
      if (actionType === "NONE") return { ...state, eventsHistory: updatedHistory };
      return {
        ...state,
        status,
        currentTurn: {
          actionType,
          playerId: activeId,
          isMyTurn: activeId === myPlayerId,
        },
        eventsHistory: updatedHistory,
      };
    }

    case "WaitingForBid": {
      const bidderId = destinationPlayerId;
      if (bidderId === undefined) return { ...state, eventsHistory: updatedHistory };
      // Il backend comunica la puntata vietata per l'ultimo bidder
      // (somma puntate != round). Va mostrata nel GameTurnBanner.
      const invalidBid = parseInvalidBidField(fields.invalidBid);
      return {
        ...state,
        status: "BIDDING",
        currentTurn: {
          actionType: "BID",
          playerId: bidderId,
          isMyTurn: bidderId === myPlayerId,
        },
        invalidBid,
        eventsHistory: updatedHistory,
      };
    }

    case "BidPlaced": {
      const bidderId = Number(eventPlayerId);
      const bidAmount = Number(fields.bid ?? 0);
      // Chi ha puntato ha finito: azzeriamo il turno in attesa del TurnOf
      // broadcast per il prossimo bidder (i WaitingForBid sono privati e gli
      // altri client non li ricevono). Così la bid scompare subito e non si
      // può piazzarne un'altra.
      const isTurnHolder = state.currentTurn.playerId === bidderId;
      return {
        ...state,
        bids: {
          ...state.bids,
          [bidderId]: bidAmount,
        },
        currentTurn: isTurnHolder
          ? { actionType: "NONE", playerId: null, isMyTurn: false }
          : state.currentTurn,
        lastError: null,
        invalidBid: null,
        eventsHistory: updatedHistory,
      };
    }

    case "WaitingForCard": {
      const activePlayerId = destinationPlayerId;
      if (activePlayerId === undefined) return { ...state, eventsHistory: updatedHistory };
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

      // Idempotenza: dopo un restore da snapshot lo stesso evento potrebbe
      // essere riapplicato (baseline approssimata). Evita doppioni sul tavolo.
      const alreadyOnTable = state.table.some(
        (entry) => entry.playerId === cardPlayerId && cardEquals(entry.card, playedCard)
      );
      const newTable = alreadyOnTable
        ? state.table
        : [...state.table, { playerId: cardPlayerId, card: playedCard }];

      // If it was my card, remove it from my hand and clear legalCards
      let newHand = state.hand;
      let newLegal = state.legalCards;
      if (cardPlayerId === myPlayerId) {
        newHand = state.hand.filter((c) => !cardEquals(c, playedCard));
        newLegal = [];
      }

      // Chi ha giocato ha finito: azzeriamo il turno in attesa del TurnOf
      // broadcast (i WaitingForCard sono privati).
      const isTurnHolder = state.currentTurn.playerId === cardPlayerId;

      return {
        ...state,
        table: newTable,
        winningCard,
        followingColor,
        hand: newHand,
        legalCards: newLegal,
        currentTurn: isTurnHolder
          ? { actionType: "NONE", playerId: null, isMyTurn: false }
          : state.currentTurn,
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
        invalidBid: null,
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
        invalidBid: null,
        eventsHistory: updatedHistory,
      };
    }

    case "ActionFailed": {
      const targetDest =
        (event.destinationId ??
          event.playerId ??
          fields.destinationId ??
          fields.playerId) as number | undefined;
      const isForMe = targetDest === myPlayerId;
      if (!isForMe) return { ...state, eventsHistory: updatedHistory };

      const reasonObj = fields.reason as
        | { error?: unknown; round?: unknown; bid?: unknown }
        | undefined;
      const errorTag = typeof reasonObj?.error === "string" ? reasonObj.error : "";
      let errorMsg: string;
      if (errorTag === "InvalidBid") {
        const round = Number(reasonObj?.round ?? state.round);
        const bid = Number(reasonObj?.bid ?? NaN);
        errorMsg = Number.isNaN(bid)
          ? `Puntata non valida al Round ${round} — la somma delle puntate non può essere uguale al numero di carte (${round}).`
          : formatInvalidBidError(round, bid);
      } else if (errorTag.includes("InvalidBid")) {
        errorMsg = `Puntata non valida al Round ${state.round} — la somma delle puntate non può essere uguale al numero di carte (${state.round}).`;
      } else if (errorTag) {
        errorMsg = `Azione non valida: ${errorTag}`;
      } else {
        errorMsg = "Azione non valida.";
      }

      return {
        ...state,
        lastError: errorMsg,
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
