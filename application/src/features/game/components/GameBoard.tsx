"use client";

import { useMemo, useRef, useState } from "react";
import { useRouter } from "next/navigation";
import { useGameBoard, TRICK_REVEAL_SECONDS } from "../hooks/useGameBoard";
import { cardEquals } from "../state/gameReducer";
import type { Card } from "../types";
import { GameActionControls } from "./GameActionControls";
import { GameCardView } from "./GameCardView";
import { GameHeader } from "./GameHeader";
import { GameScoreboard } from "./GameScoreboard";
import { GameTurnBanner } from "./GameTurnBanner";
import { PlayerHand } from "./PlayerHand";
import { TrumpArea } from "./TrumpArea";
import { GameEndOverlay } from "./GameEndOverlay";
import { Badge } from "@/ui/components/badge";

type SeatLayout = {
  pos: string;
  dir: "flex-col" | "flex-row";
  cardFirst: boolean;
};

const SEAT_BOTTOM: SeatLayout = {
  pos: "bottom-0 left-1/2 -translate-x-1/2 translate-y-1/4",
  dir: "flex-col",
  cardFirst: true,
};
const SEAT_TOP: SeatLayout = {
  pos: "top-0 left-1/2 -translate-x-1/2 -translate-y-1/4",
  dir: "flex-col",
  cardFirst: false,
};

const SEAT_PRESETS: Record<number, SeatLayout[]> = {
  2: [SEAT_BOTTOM, SEAT_TOP],
  3: [
    SEAT_BOTTOM,
    { pos: "left-0 top-[38%] -translate-x-1/4 -translate-y-1/2", dir: "flex-row", cardFirst: false },
    { pos: "right-0 top-[38%] translate-x-1/4 -translate-y-1/2", dir: "flex-row", cardFirst: true },
  ],
  4: [
    SEAT_BOTTOM,
    { pos: "left-0 top-1/2 -translate-x-1/4 -translate-y-1/2", dir: "flex-row", cardFirst: false },
    SEAT_TOP,
    { pos: "right-0 top-1/2 translate-x-1/4 -translate-y-1/2", dir: "flex-row", cardFirst: true },
  ],
  5: [
    SEAT_BOTTOM,
    { pos: "left-0 top-[55%] -translate-x-1/4 -translate-y-1/2", dir: "flex-row", cardFirst: false },
    { pos: "left-[24%] top-0 -translate-x-1/4 -translate-y-1/4", dir: "flex-col", cardFirst: false },
    { pos: "left-[76%] top-0 translate-x-1/4 -translate-y-1/4", dir: "flex-col", cardFirst: false },
    { pos: "right-0 top-[55%] translate-x-1/4 -translate-y-1/2", dir: "flex-row", cardFirst: true },
  ],
  6: [
    SEAT_BOTTOM,
    { pos: "left-0 top-[55%] -translate-x-1/4 -translate-y-1/2", dir: "flex-row", cardFirst: false },
    { pos: "left-[20%] top-0 -translate-x-1/4 -translate-y-1/4", dir: "flex-col", cardFirst: false },
    SEAT_TOP,
    { pos: "left-[80%] top-0 translate-x-1/4 -translate-y-1/4", dir: "flex-col", cardFirst: false },
    { pos: "right-0 top-[55%] translate-x-1/4 -translate-y-1/2", dir: "flex-row", cardFirst: true },
  ],
};

function getSeatLayout(idx: number, total: number): SeatLayout {
  const preset = SEAT_PRESETS[Math.min(Math.max(total, 2), 6)] ?? SEAT_PRESETS[6];
  return preset[idx % preset.length];
}

interface GameBoardProps {
  customPlayerId?: number;
}

export function GameBoard({ customPlayerId }: GameBoardProps) {
  const router = useRouter();
  const {
    lobbyId,
    playerId,
    connectionState,
    lobby,
    playersMap,
    gameState,
    revealedTrick,
    revealSecondsLeft,
    isMyTurn,
    canChooseTrump,
    canBid,
    canPlay,
    turnPrompt,
    isCardPlayable,
    forbiddenBid,
    bidsTotal,
    bidWarning,
    bidError,
    actionError,
    lobbyWarning,
    selectedCard,
    setSelectedCard,
    bidInput,
    setBidInput,
    selectedColor,
    setSelectedColor,
    isSubmitting,
    actionStatus,
    isRestoring,
    snapshotError,
    refreshSnapshot,
    handleChooseTrump,
    handlePlaceBid,
    handlePlayCard,
  } = useGameBoard(customPlayerId);

  const [isCardDragging, setIsCardDragging] = useState(false);
  const tableRef = useRef<HTMLDivElement | null>(null);

  const players = lobby?.players ?? [];
  const myIndex = players.findIndex((p) => p.id === playerId);
  const orderedPlayers = useMemo(
    () => (myIndex !== -1 ? [...players.slice(myIndex), ...players.slice(0, myIndex)] : players),
    [players, myIndex]
  );

  const isGameEnded = gameState.status === "GAME_ENDED";

  const sortedScoreboard = useMemo(() => {
    if (!gameState.scoreboard) return [];
    return Object.entries(gameState.scoreboard)
      .map(([pIdStr, entries]) => {
        const pId = Number(pIdStr);
        const playerObj = playersMap.get(pId);
        const playerName =
          typeof playerObj === "object" && playerObj !== null
            ? playerObj.name
            : playerObj ?? `Giocatore ${pId}`;

        let finalScore = 0;
        if (Array.isArray(entries) && entries.length > 0) {
          finalScore = Number(entries[entries.length - 1]?.score ?? 0);
        } else if (typeof entries === "number") {
          finalScore = entries;
        } else if (entries && typeof entries === "object" && "score" in entries) {
          finalScore = Number((entries as { score: unknown }).score ?? 0);
        }

        return {
          id: pId,
          name: playerName,
          score: Number.isNaN(finalScore) ? 0 : finalScore,
        };
      })
      .sort((a, b) => b.score - a.score);
  }, [gameState.scoreboard, playersMap]);

  const handleReturnToLobby = () => {
    router.push(lobbyId ? `/lobby/${lobbyId}` : "/");
  };

  const handleDropCard = (card: Card) => {
    setSelectedCard(null);
    void handlePlayCard(card);
  };

  return (
    <div className="relative w-full max-w-7xl mx-auto space-y-6 pb-20 px-2 sm:px-4">
      <GameEndOverlay
        isGameEnded={isGameEnded}
        sortedScoreboard={sortedScoreboard}
        playerId={playerId}
        onReturnToLobby={handleReturnToLobby}
      />

      <GameHeader
        lobbyId={lobbyId}
        playerId={playerId}
        connectionState={connectionState}
        round={gameState.round}
        status={gameState.status}
      />

      {isRestoring && (
        <div className="p-3 rounded-2xl border border-amber-400/50 bg-amber-950/60 text-amber-200 text-sm font-semibold text-center animate-pulse">
          🔄 Ripristino stato partita dal server...
        </div>
      )}
      {snapshotError && !isRestoring && (
        <div className="p-3 rounded-2xl border border-rose-700/60 bg-rose-950/60 text-rose-200 text-sm text-center space-y-2">
          <p className="font-semibold">⚠️ {snapshotError}</p>
          <button
            type="button"
            onClick={() => void refreshSnapshot("manual")}
            className="px-4 py-1.5 rounded-xl bg-rose-500 text-white text-xs font-bold uppercase tracking-wider hover:bg-rose-400 transition-colors"
          >
            Riprova sincronizzazione
          </button>
        </div>
      )}

      <div className="mx-auto w-full max-w-md">
        <GameTurnBanner
          isMyTurn={isMyTurn}
          turnPrompt={turnPrompt}
          lastError={gameState.lastError ?? bidError ?? actionError}
          actionStatus={actionStatus}
          bidWarning={bidWarning}
          statusWarning={lobbyWarning}
        />
      </div>

      <div
        ref={tableRef}
        className={`relative w-full my-10 py-10 px-4 min-h-[560px] sm:min-h-[600px] rounded-[120px] sm:rounded-[180px] bg-gradient-to-b from-emerald-900 via-emerald-800 to-emerald-950 border-[12px] border-amber-950/80 shadow-[inset_0_0_80px_rgba(0,0,0,0.8),0_20px_50px_rgba(0,0,0,0.6)] transition-shadow duration-200 ${
          isCardDragging ? "ring-4 ring-emerald-300/80" : ""
        }`}
      >
        {isCardDragging && (
          <div className="pointer-events-none absolute top-5 left-1/2 z-30 -translate-x-1/2 animate-pulse rounded-full border border-emerald-300/60 bg-emerald-500/20 px-4 py-1.5 text-[11px] font-black tracking-widest whitespace-nowrap text-emerald-100 uppercase">
            Rilascia qui per giocare
          </div>
        )}

        <div className="absolute inset-4 rounded-[100px] sm:rounded-[160px] border-2 border-emerald-600/30 pointer-events-none flex items-center justify-center">
          <span className="text-emerald-900/20 text-6xl sm:text-8xl font-black uppercase tracking-widest select-none">
            WIZARD
          </span>
        </div>

        <div className="absolute top-1/2 left-1/2 z-10 flex -translate-x-1/2 -translate-y-1/2 flex-col items-center gap-2">
          <TrumpArea trump={gameState.trump} effectiveTrumpColor={gameState.effectiveTrumpColor} />
          {gameState.followingColor && (
            <span className="rounded-full border border-emerald-600/40 bg-emerald-950/70 px-2.5 py-0.5 text-[10px] font-bold text-emerald-300">
              Seme di mano: {gameState.followingColor}
            </span>
          )}
        </div>

        {revealedTrick && (
          <div className="pointer-events-none absolute top-1/2 left-1/2 z-30 flex w-max max-w-[92%] -translate-x-1/2 -translate-y-1/2 flex-col items-center gap-2 rounded-3xl border border-amber-400/60 bg-zinc-950/95 p-3 shadow-2xl backdrop-blur-md sm:p-4">
            <span className="rounded-full border border-amber-400/50 bg-amber-500/15 px-3 py-0.5 text-[11px] font-black tracking-wider text-amber-300 uppercase">
              ★ Presa di {playersMap.get(revealedTrick.winnerId)?.name ?? `Giocatore ${revealedTrick.winnerId}`}
            </span>
            <div className="flex max-w-full flex-wrap items-start justify-center gap-2">
              {revealedTrick.entries.map((entry, index) => {
                const entryName = playersMap.get(entry.playerId)?.name ?? `Giocatore ${entry.playerId}`;
                const entryWinning = Boolean(
                  revealedTrick.winningCard && cardEquals(entry.card, revealedTrick.winningCard)
                );
                return (
                  <div key={`${entry.playerId}-${index}`} className="flex flex-col items-center gap-1">
                    <div className={entryWinning ? "rounded-xl ring-2 ring-amber-400 ring-offset-2 ring-offset-zinc-950" : ""}>
                      <GameCardView card={entry.card} size="sm" isClickable={false} />
                    </div>
                    <span className={`max-w-16 truncate text-[9px] font-bold ${entryWinning ? "text-amber-300" : "text-zinc-400"}`}>
                      {entryName}
                    </span>
                  </div>
                );
              })}
            </div>
            <span className="animate-pulse font-mono text-[11px] font-bold text-zinc-300">
              Si continua tra {revealSecondsLeft}s…
            </span>
            <div className="h-1 w-full overflow-hidden rounded-full bg-white/10">
              <div
                className="h-full rounded-full bg-amber-400 transition-[width] duration-1000 ease-linear"
                style={{ width: `${Math.max(0, Math.min(100, (revealSecondsLeft / TRICK_REVEAL_SECONDS) * 100))}%` }}
              />
            </div>
          </div>
        )}
        {orderedPlayers.map((player, idx) => {
          const isCurrentTurn = gameState.currentTurn.playerId === player.id;
          const isMe = player.id === playerId;
          const playerBid = gameState.bids[player.id];
          const playerTricks = gameState.tricksWon[player.id] ?? 0;
          const isBot = Boolean(player.difficulty);
          const seat = getSeatLayout(idx, orderedPlayers.length);
          const played = gameState.table.find((entry) => entry.playerId === player.id);
          const isWinning = Boolean(
            played && gameState.winningCard && cardEquals(played.card, gameState.winningCard)
          );
          const showSlot = played !== undefined || gameState.table.length > 0;

          const badge = (
            <div
              className={`flex items-center gap-2 rounded-2xl border px-2 py-1.5 shadow-xl backdrop-blur-md transition-all duration-300 sm:px-2.5 sm:py-2 ${
                isCurrentTurn
                  ? "border-amber-400 bg-amber-500/20 ring-2 ring-amber-400/60"
                  : "border-zinc-700/80 bg-zinc-900/90"
              }`}
            >
              <span
                className={`grid size-8 shrink-0 place-items-center rounded-full text-xs font-black ${
                  isMe ? "bg-amber-400 text-zinc-950" : "bg-zinc-700 text-zinc-100"
                }`}
              >
                {player.name.charAt(0).toUpperCase()}
              </span>
              <span className="min-w-0">
                <span className="flex items-center gap-1">
                  <span className="max-w-[84px] truncate text-xs font-bold text-white sm:max-w-[110px]">
                    {player.name}
                  </span>
                  {isMe && <Badge variant="secondary" className="px-1 py-0 text-[9px]">TU</Badge>}
                  {isBot && <Badge variant="outline" className="px-1 py-0 text-[9px] text-zinc-400">BOT</Badge>}
                </span>
                <span className="mt-0.5 flex items-center gap-2 font-mono text-[10px] text-zinc-400">
                  <span>Bid <strong className="text-amber-400">{playerBid !== undefined ? playerBid : "-"}</strong></span>
                  <span>Trick <strong className="text-emerald-400">{playerTricks}</strong></span>
                </span>
                {isCurrentTurn && (
                  <span className="mt-0.5 block animate-pulse text-[9px] font-extrabold tracking-wider text-amber-300 uppercase">
                    {isMe ? "▶ tocca a te" : "▶ turno"}
                  </span>
                )}
              </span>
            </div>
          );

          const slot = showSlot ? (
            played ? (
              <div className="flex origin-center scale-[0.8] flex-col items-center gap-1 sm:scale-100">
                <div className={isWinning ? "rounded-xl ring-2 ring-amber-400 ring-offset-2 ring-offset-emerald-900" : ""}>
                  <GameCardView card={played.card} size="md" isClickable={false} />
                </div>
                {isWinning && (
                  <span className="animate-pulse rounded border border-amber-400/60 bg-amber-500/20 px-1.5 py-0.5 text-[9px] font-black tracking-wider text-amber-300 uppercase">
                    ★ in testa
                  </span>
                )}
              </div>
            ) : (
              <div className="grid h-28 w-20 origin-center scale-[0.8] place-items-center rounded-xl border-2 border-dashed border-white/15 bg-black/25 sm:scale-100">
                <span className="animate-pulse text-lg text-white/25">…</span>
              </div>
            )
          ) : null;

          return (
            <div key={player.id} className={`absolute z-20 ${seat.pos}`}>
              <div className={`flex items-center gap-2 sm:gap-3 ${seat.dir}`}>
                {seat.cardFirst ? (<>{slot}{badge}</>) : (<>{badge}{slot}</>)}
              </div>
            </div>
          );
        })}
      </div>

      {/* 3. Mano del Giocatore & Controlli d'Azione */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6 items-start">
        <div className="lg:col-span-2">
          <PlayerHand
            hand={gameState.hand}
            selectedCard={selectedCard}
            canPlay={canPlay}
            isSubmitting={isSubmitting}
            isCardPlayable={isCardPlayable}
            onSelectCard={setSelectedCard}
            dropZoneRef={tableRef}
            onDropCard={handleDropCard}
            onDragStateChange={setIsCardDragging}
          />
        </div>

        <div>
          <GameActionControls
            isMyTurn={isMyTurn}
            canChooseTrump={canChooseTrump}
            canBid={canBid}
            canPlay={canPlay}
            round={gameState.round}
            selectedColor={selectedColor}
            onSelectColor={setSelectedColor}
            onChooseTrump={handleChooseTrump}
            bidInput={bidInput}
            onSelectBid={setBidInput}
            onPlaceBid={handlePlaceBid}
            isSubmitting={isSubmitting}
            forbiddenBid={forbiddenBid}
            bidsTotal={bidsTotal}
          />
        </div>
      </div>

      {/* 4. Pannelli Secondari & Dev Tool */}
      <div className="space-y-6 pt-4 border-t border-zinc-800">
        <GameScoreboard
          scoreboard={gameState.scoreboard}
          playersMap={playersMap}
          myPlayerId={playerId}
        />
      </div>
    </div>
  );
}