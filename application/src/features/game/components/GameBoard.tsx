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
import { Button } from "@/ui/components/button";
import { Lightbulb, X } from "lucide-react";

type SeatLayout = {
  pos: string;
  dir: "flex-col" | "flex-row";
  cardFirst: boolean;
};

const SEAT_PRESETS: Record<number, SeatLayout[]> = {
  2: [
    { pos: "bottom-1 left-1/2 -translate-x-1/2", dir: "flex-col", cardFirst: true },
    { pos: "top-1 left-1/2 -translate-x-1/2", dir: "flex-col", cardFirst: false },
  ],
  3: [
    { pos: "bottom-1 left-1/2 -translate-x-1/2", dir: "flex-col", cardFirst: true },
    { pos: "top-[25%] left-1 -translate-y-1/2", dir: "flex-row", cardFirst: false },
    { pos: "top-[25%] right-1 -translate-y-1/2", dir: "flex-row", cardFirst: true },
  ],
  4: [
    { pos: "bottom-1 left-1/2 -translate-x-1/2", dir: "flex-col", cardFirst: true },
    { pos: "top-1/2 left-1 -translate-y-1/2", dir: "flex-row", cardFirst: false },
    { pos: "top-1 left-1/2 -translate-x-1/2", dir: "flex-col", cardFirst: false },
    { pos: "top-1/2 right-1 -translate-y-1/2", dir: "flex-row", cardFirst: true },
  ],
  5: [
    { pos: "bottom-1 left-1/2 -translate-x-1/2", dir: "flex-col", cardFirst: true },
    { pos: "top-[62%] left-1 -translate-y-1/2", dir: "flex-row", cardFirst: false },
    { pos: "top-[12%] left-[18%] -translate-x-1/2", dir: "flex-col", cardFirst: false },
    { pos: "top-[12%] right-[18%] translate-x-1/2", dir: "flex-col", cardFirst: false },
    { pos: "top-[62%] right-1 -translate-y-1/2", dir: "flex-row", cardFirst: true },
  ],
  6: [
    { pos: "bottom-1 left-1/2 -translate-x-1/2", dir: "flex-col", cardFirst: true },
    { pos: "top-[65%] left-1 -translate-y-1/2", dir: "flex-row", cardFirst: false },
    { pos: "top-[15%] left-[12%] -translate-x-1/2", dir: "flex-col", cardFirst: false },
    { pos: "top-1 left-1/2 -translate-x-1/2", dir: "flex-col", cardFirst: false },
    { pos: "top-[15%] right-[12%] translate-x-1/2", dir: "flex-col", cardFirst: false },
    { pos: "top-[65%] right-1 -translate-y-1/2", dir: "flex-row", cardFirst: true },
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
    turnTimerSeconds,
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
    hintedCard,
    hintedBid,
    isHintLoading,
    hintError,
    canRequestHint,
    requestHint,
  } = useGameBoard(customPlayerId);

  const [isCardDragging, setIsCardDragging] = useState(false);
  const [isReturning, setIsReturning] = useState(false);
  const [isPausing, setIsPausing] = useState(false);
  const [pauseError, setPauseError] = useState<string | null>(null);
  const [showScoreboard, setShowScoreboard] = useState(false);
  const tableRef = useRef<HTMLDivElement | null>(null);

  const players = lobby?.players ?? [];
  const myIndex = players.findIndex((p) => p.id === playerId);
  const orderedPlayers = useMemo(
    () => (myIndex !== -1 ? [...players.slice(myIndex), ...players.slice(0, myIndex)] : players),
    [players, myIndex]
  );

  const activeTurnPlayer = useMemo(
    () => players.find((p) => p.id === gameState.currentTurn.playerId),
    [players, gameState.currentTurn.playerId]
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

  const handleReturnToLobby = async () => {
    if (isReturning) return;
    setIsReturning(true);
    try {
      const { discardPausedGameAction } = await import("@/features/lobby/api");
      await discardPausedGameAction(lobbyId);
    } catch {}
    router.push(lobbyId ? `/lobby/${lobbyId}` : "/");
  };

  const handleBackToLobby = async () => {
    if (isPausing || !lobbyId) return;
    setPauseError(null);
    if (lobby?.status === "IN_GAME" || lobby?.status === "DISCONNECTING") {
      setIsPausing(true);
      try {
        const { pauseGameAction } = await import("@/features/lobby/api");
        const { getErrorMessage } = await import("@/ui/i18n/errors");
        const result = await pauseGameAction(lobbyId);
        if (result.error) {
          setPauseError(getErrorMessage(result.error));
          setIsPausing(false);
          return;
        }
      } catch {
        setPauseError("Impossibile mettere in pausa la partita.");
        setIsPausing(false);
        return;
      }
      setIsPausing(false);
    }
    router.push(`/lobby/${lobbyId}`);
  };

  const handleDropCard = (card: Card) => {
    setSelectedCard(null);
    void handlePlayCard(card);
  };

  const hasActiveControls = isMyTurn && (canBid || canChooseTrump);

  return (
    <div className="relative flex flex-col min-h-screen w-full max-w-md sm:max-w-4xl lg:max-w-6xl mx-auto px-2 pb-6 space-y-2 sm:space-y-3 select-none overflow-x-hidden">
      <GameEndOverlay
        isGameEnded={isGameEnded}
        sortedScoreboard={sortedScoreboard}
        playerId={playerId}
        onReturnToLobby={() => void handleReturnToLobby()}
        isReturning={isReturning}
      />

      <GameHeader
        lobbyId={lobbyId}
        playerId={playerId}
        connectionState={connectionState}
        round={gameState.round}
        status={gameState.status}
        isPausing={isPausing}
        onBackToLobby={() => void handleBackToLobby()}
        onToggleScoreboard={() => setShowScoreboard((prev) => !prev)}
      />

      {pauseError && (
        <div className="p-2 rounded-xl border border-rose-700/60 bg-rose-950/80 text-rose-200 text-xs text-center">
          <p className="font-semibold">⚠️ {pauseError}</p>
        </div>
      )}

      {isRestoring && (
        <div className="p-2 rounded-xl border border-amber-400/50 bg-amber-950/80 text-amber-200 text-xs font-semibold text-center animate-pulse">
          🔄 Ripristino stato partita dal server...
        </div>
      )}

      {snapshotError && !isRestoring && (
        <div className="p-2 rounded-xl border border-rose-700/60 bg-rose-950/80 text-rose-200 text-xs text-center space-y-1">
          <p className="font-semibold">⚠️ {snapshotError}</p>
          <button
            type="button"
            onClick={() => void refreshSnapshot("manual")}
            className="px-3 py-1 rounded-lg bg-rose-500 text-white text-[10px] font-bold uppercase tracking-wider"
          >
            Riprova sincronizzazione
          </button>
        </div>
      )}

      {/* Banner Turno */}
      {/* <div className="w-full">
        <GameTurnBanner
          isMyTurn={isMyTurn}
          turnPrompt={turnPrompt}
          lastError={gameState.lastError ?? bidError ?? actionError}
          actionStatus={actionStatus}
          bidWarning={bidWarning}
          statusWarning={lobbyWarning}
          turnTimerSeconds={turnTimerSeconds}
          strikes={activeTurnPlayer?.strikes ?? 0}
        />
      </div> */}

      {/* Board Tonda */}
      <div
        ref={tableRef}
        className={`relative w-full transition-all duration-300 mx-auto rounded-full bg-gradient-to-b from-emerald-900 via-emerald-800 to-emerald-950 border-[6px] sm:border-[10px] border-amber-950/90 shadow-[inset_0_0_50px_rgba(0,0,0,0.85),0_10px_30px_rgba(0,0,0,0.5)] ${
          hasActiveControls
            ? "h-[300px] sm:h-[400px] max-w-[340px] sm:max-w-[480px]"
            : "h-[360px] sm:h-[480px] max-w-[400px] sm:max-w-[580px]"
        } ${isCardDragging ? "ring-4 ring-emerald-300/80" : ""}`}
      >
        {isCardDragging && (
          <div className="pointer-events-none absolute top-3 left-1/2 z-30 -translate-x-1/2 animate-pulse rounded-full border border-emerald-300/60 bg-emerald-500/30 px-3 py-0.5 text-[9px] font-black tracking-widest text-emerald-100 uppercase backdrop-blur-sm">
            Rilascia qui
          </div>
        )}

        <div className="absolute inset-2 sm:inset-4 rounded-full border border-emerald-600/30 pointer-events-none flex items-center justify-center">
          <span className="text-emerald-900/20 text-3xl sm:text-6xl font-black uppercase tracking-widest select-none">
            WIZARD
          </span>
        </div>

        {/* Area Briscola Centrata */}
        <div className="absolute top-1/2 left-1/2 z-10 flex -translate-x-1/2 -translate-y-1/2 flex-col items-center gap-1 scale-85 sm:scale-100">
          <TrumpArea trump={gameState.trump} effectiveTrumpColor={gameState.effectiveTrumpColor} />
          {gameState.followingColor && (
            <span className="rounded-full border border-emerald-600/40 bg-emerald-950/80 px-2 py-0.5 text-[8px] sm:text-[10px] font-bold text-emerald-300 shadow">
              Seme: {gameState.followingColor}
            </span>
          )}
        </div>

        {/* Postazioni Giocatori */}
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
              className={`flex items-center gap-1 rounded-lg border px-1.5 py-0.5 shadow-md backdrop-blur-md transition-all duration-200 ${
                isCurrentTurn
                  ? "border-amber-400 bg-amber-500/30 ring-2 ring-amber-400/60 scale-105"
                  : "border-zinc-800 bg-zinc-950/85"
              }`}
            >
              <span
                className={`grid size-5 shrink-0 place-items-center rounded-full text-[9px] font-black ${
                  isMe ? "bg-amber-400 text-zinc-950" : "bg-zinc-700 text-zinc-100"
                }`}
              >
                {player.name.charAt(0).toUpperCase()}
              </span>
              <span className="min-w-0">
                <span className="flex items-center gap-0.5">
                  <span className="max-w-[45px] sm:max-w-[70px] truncate text-[9px] font-bold text-white">
                    {player.name}
                  </span>
                  {isMe && <Badge variant="secondary" className="px-0.5 py-0 text-[7px]">TU</Badge>}
                  {isBot && <Badge variant="outline" className="px-0.5 py-0 text-[7px] text-zinc-400">BOT</Badge>}
                </span>
                <span className="flex items-center gap-1 font-mono text-[8px] text-zinc-400">
                  <span>B:<strong className="text-amber-400">{playerBid ?? "-"}</strong></span>
                  <span>T:<strong className="text-emerald-400">{playerTricks}</strong></span>
                </span>
                {isCurrentTurn && (
                  <span className="flex items-center gap-1 font-mono text-[8px] font-black mt-0.5">
                    {turnTimerSeconds !== null && turnTimerSeconds !== undefined && (
                      <span className={turnTimerSeconds <= 5 ? "text-rose-400 animate-pulse" : "text-amber-300"}>
                        ⏱{turnTimerSeconds}s
                      </span>
                    )}
                    <span className={(player.strikes ?? 0) > 0 ? "text-rose-300" : "text-zinc-400"}>
                      ⚡{player.strikes ?? 0}
                    </span>
                  </span>
                )}
              </span>
            </div>
          );

          const slot = showSlot ? (
            played ? (
              <div className="flex origin-center scale-70 sm:scale-85 flex-col items-center">
                <div className={isWinning ? "rounded-lg ring-2 ring-amber-400" : ""}>
                  <GameCardView card={played.card} size="sm" isClickable={false} />
                </div>
              </div>
            ) : (
              <div className="grid h-14 w-10 origin-center scale-70 sm:scale-85 place-items-center rounded-lg border border-dashed border-white/20 bg-black/30">
                <span className="animate-pulse text-[10px] text-white/30">…</span>
              </div>
            )
          ) : null;

          return (
            <div key={player.id} className={`absolute z-20 ${seat.pos}`}>
              <div className={`flex items-center gap-1 ${seat.dir}`}>
                {seat.cardFirst ? (<>{slot}{badge}</>) : (<>{badge}{slot}</>)}
              </div>
            </div>
          );
        })}
      </div>

      {/* Controlli Azione */}
      {hasActiveControls && (
        <div className="w-full">
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
            hintedBid={hintedBid}
          />
        </div>
      )}

      {/* Presa Rivelata nello spazio tra il tavolo e la mano */}
      {revealedTrick && (
        <div className="w-full flex justify-center z-30 my-1 animate-in fade-in duration-200">
          <div className="pointer-events-none flex w-max max-w-[95%] flex-col items-center gap-1 rounded-2xl border border-amber-400/60 bg-zinc-950/95 p-2 sm:p-3 shadow-2xl backdrop-blur-md">
            <span className="rounded-full border border-amber-400/50 bg-amber-500/15 px-2 py-0.5 text-[9px] font-black tracking-wider text-amber-300 uppercase">
              ★ Presa: {playersMap.get(revealedTrick.winnerId)?.name ?? `P${revealedTrick.winnerId}`}
            </span>
            <div className="flex max-w-full flex-wrap items-start justify-center gap-1">
              {revealedTrick.entries.map((entry, index) => {
                const entryName = playersMap.get(entry.playerId)?.name ?? `P${entry.playerId}`;
                const entryWinning = Boolean(
                  revealedTrick.winningCard && cardEquals(entry.card, revealedTrick.winningCard)
                );
                return (
                  <div key={`${entry.playerId}-${index}`} className="flex flex-col items-center gap-0.5">
                    <div className={entryWinning ? "rounded-lg ring-2 ring-amber-400" : ""}>
                      <GameCardView card={entry.card} size="sm" isClickable={false} />
                    </div>
                    <span className={`max-w-10 truncate text-[8px] font-bold ${entryWinning ? "text-amber-300" : "text-zinc-400"}`}>
                      {entryName}
                    </span>
                  </div>
                );
              })}
            </div>
            <span className="animate-pulse font-mono text-[9px] text-zinc-300">
              Prossimo tra {revealSecondsLeft}s…
            </span>
          </div>
        </div>
      )}

      {/* Contenitore Mano Giocatore con Bottone Hint Tondo in Alto a Destra */}
      <div className="w-full mt-auto overflow-visible relative flex flex-col items-end">
        {canRequestHint && (
          <div className="pr-2 sm:pr-4 -mb-10 z-30 flex flex-col items-end gap-1">
            {hintError && (
              <div className="max-w-xs text-[10px] font-semibold text-rose-200 bg-rose-950/90 border border-rose-500/60 rounded-xl px-2.5 py-1 shadow-2xl backdrop-blur-md">
                ⚠️ {hintError}
              </div>
            )}
            <Button
              type="button"
              size="icon"
              disabled={isSubmitting || isHintLoading}
              onClick={() => void requestHint()}
              title={
                canPlay
                  ? "Suggerisci Carta"
                  : canBid
                  ? "Suggerisci Puntata"
                  : canChooseTrump
                  ? "Suggerisci Briscola"
                  : "Suggerimento"
              }
              className="size-11 sm:size-12 rounded-full bg-gradient-to-r from-amber-500 via-amber-400 to-amber-500 hover:from-amber-400 hover:to-amber-300 text-zinc-950 font-black shadow-xl shadow-amber-500/20 border-2 border-amber-300/80 p-0 flex items-center justify-center transition-all hover:scale-110 active:scale-95"
            >
              <Lightbulb className={`size-5 sm:size-6 ${isHintLoading ? "animate-spin text-zinc-950" : "text-zinc-950 fill-zinc-950"}`} />
            </Button>
          </div>
        )}

        <PlayerHand
          hand={gameState.hand}
          selectedCard={selectedCard}
          canPlay={canPlay}
          isSubmitting={isSubmitting}
          isCardPlayable={isCardPlayable}
          onSelectCard={setSelectedCard}
          hintedCard={hintedCard}
          dropZoneRef={tableRef}
          onDropCard={handleDropCard}
          onDragStateChange={setIsCardDragging}
        />
      </div>

      {/* Modal Tabellone Scoreboard */}
      {showScoreboard && (
        <div
          className="fixed inset-0 z-50 flex items-center justify-center bg-black/75 backdrop-blur-sm p-4 animate-in fade-in duration-200"
          onClick={() => setShowScoreboard(false)}
        >
          <div
            className="w-full max-w-lg"
            onClick={(e) => e.stopPropagation()}
          >
            <GameScoreboard
              scoreboard={gameState.scoreboard}
              playersMap={playersMap}
              myPlayerId={playerId}
              onClose={() => setShowScoreboard(false)}
            />
          </div>
        </div>
      )}
    </div>
  );
}