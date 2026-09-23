"use client";

import Confetti from "react-confetti";
import { useWindowSize } from "react-use";
import { GameScoreboard } from "./GameScoreboard";
import { Trophy, ArrowLeft, Loader2 } from "lucide-react";
import type { Scoreboard } from "../types";
import { t } from "@/ui/i18n/core";

const lobbyI18n = t("lobby");

export interface ScoreItem {
  id: number;
  name: string;
  score: number;
}

interface GameEndOverlayProps {
  isGameEnded: boolean;
  sortedScoreboard: ScoreItem[];
  scoreboard?: Scoreboard | null;
  players?: any[];
  playersMap?: Map<number, { name: string; isOnline?: boolean }>;
  playerId?: number | null;
  onReturnToLobby: () => void;
  isReturning?: boolean;
}

export function GameEndOverlay({
  isGameEnded,
  sortedScoreboard,
  scoreboard,
  players,
  playersMap,
  playerId,
  onReturnToLobby,
  isReturning,
}: GameEndOverlayProps) {
  const { width, height } = useWindowSize();

  if (!isGameEnded) return null;

  // Costruiamo una fallback map se non viene passata direttamente
  const effectivePlayersMap =
    playersMap ??
    new Map(
      sortedScoreboard.map((item) => [
        item.id,
        { name: item.name, isOnline: true },
      ])
    );

  // Costruiamo un oggetto scoreboard di fallback se non presente
  const effectiveScoreboard: Scoreboard =
    scoreboard ??
    Object.fromEntries(
      sortedScoreboard.map((item) => [
        item.id,
        [{ round: 1, score: item.score, bid: 0, tricks: 0 }],
      ])
    );

  const winner = sortedScoreboard[0];

  return (
    <div className="fixed inset-0 z-[100] grid place-items-center bg-zinc-950/80 backdrop-blur-md p-3 sm:p-6 overflow-y-auto">
      <Confetti
        width={width || 1920}
        height={height || 1080}
        numberOfPieces={250}
        recycle={false}
        style={{ zIndex: 110, position: "fixed", top: 0, left: 0 }}
      />

      <div className="relative w-full max-w-lg bg-zinc-900/90 border border-zinc-800 shadow-2xl rounded-2xl overflow-hidden flex flex-col z-10 my-auto">
        {/* Intestazione stile Lobby */}
        <div className="bg-zinc-950/60 p-5 sm:p-6 text-center border-b border-zinc-800 space-y-2">
          <div className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full border border-zinc-700 bg-zinc-800/60 text-zinc-300 text-[11px] font-bold uppercase tracking-wider">
            <Trophy className="size-3.5 text-zinc-200" />
            {lobbyI18n.pausedSummary.titleEnded}
          </div>

          <h2 className="text-2xl sm:text-3xl font-extrabold tracking-tighter text-transparent bg-clip-text bg-gradient-to-br from-white via-zinc-200 to-zinc-400 drop-shadow-sm uppercase">
            {lobbyI18n.pausedSummary.finalScore}
          </h2>

          {winner && (
            <p className="text-xs sm:text-sm text-zinc-400 font-medium">
              Vincitore:{" "}
              <span className="text-white font-bold">{winner.name}</span> con{" "}
              <span className="font-mono font-bold text-zinc-200">
                {winner.score} pt
              </span>
            </p>
          )}
        </div>

        {/* Scoreboard riutilizzato (non scrollabile internamente) */}
        <div className="p-3 sm:p-4 flex-1">
          <GameScoreboard
            scoreboard={effectiveScoreboard}
            playersMap={effectivePlayersMap}
            players={players}
            myPlayerId={playerId}
            isScrollable={false}
          />
        </div>

        {/* Azione principale identical alla DisconnectOverlay */}
        <div className="p-4 sm:p-5 border-t border-zinc-800 bg-zinc-950/60 flex justify-center">
          <button
            type="button"
            disabled={isReturning}
            onClick={onReturnToLobby}
            className="flex items-center gap-1.5 rounded-xl border border-zinc-600/60 bg-zinc-800/80 px-4 py-2.5 text-xs sm:text-sm font-bold text-zinc-200 transition-colors hover:bg-zinc-700 disabled:cursor-not-allowed disabled:opacity-50"
          >
            {isReturning ? (
              <Loader2 className="size-4 animate-spin" />
            ) : (
              <ArrowLeft className="size-4" />
            )}
            {lobbyI18n.disconnectOverlay.backToLobby}
          </button>
        </div>
      </div>
    </div>
  );
}