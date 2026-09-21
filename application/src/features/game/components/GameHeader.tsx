"use client";

import { Badge } from "@/ui/components/badge";
import { Button } from "@/ui/components/button";
import { ArrowLeft, Loader2, Pause } from "lucide-react";

interface GameHeaderProps {
  lobbyId: string;
  playerId: number | null;
  connectionState: string;
  round: number;
  status: string;
  canPauseExit?: boolean;
  isPausing?: boolean;
  onPauseExit?: () => void;
  isGoingHome?: boolean;
  onBackHome?: () => void;
}

export function GameHeader({
  lobbyId,
  playerId,
  connectionState,
  round,
  status,
  canPauseExit,
  isPausing,
  onPauseExit,
  isGoingHome,
  onBackHome,
}: GameHeaderProps) {
  return (
    <div className="flex flex-wrap items-center justify-between gap-3 p-3 sm:p-4 rounded-2xl bg-zinc-950/90 border border-amber-500/30 shadow-2xl backdrop-blur-md">
      <div className="flex flex-wrap items-center gap-2 sm:gap-3">
        {onBackHome && (
          <Button
            type="button"
            size="sm"
            variant="ghost"
            disabled={isGoingHome || isPausing}
            onClick={onBackHome}
            title="Torna alla home senza uscire dalla lobby (la partita resta salvata)"
            className="gap-1.5 text-zinc-400 hover:text-zinc-100 text-xs font-semibold"
          >
            {isGoingHome ? <Loader2 className="w-3.5 h-3.5 animate-spin" /> : <ArrowLeft className="w-3.5 h-3.5" />}
            Home
          </Button>
        )}
        <h1 className="text-lg sm:text-xl font-black tracking-wider text-amber-400 flex items-center gap-2">
          <span>♦</span> WIZARD POKER TABLE
        </h1>
        <Badge variant="outline" className="text-[11px] bg-zinc-900 border-zinc-700 text-zinc-300">
          Lobby: <span className="font-mono ml-1 text-amber-400">{lobbyId}</span>
        </Badge>
        <Badge variant="outline" className="text-[11px] bg-zinc-900 border-zinc-700 text-zinc-300">
          Player: <span className="font-mono ml-1 text-emerald-400">#{playerId ?? "…"}</span>
        </Badge>
        <Badge
          variant={connectionState === "open" ? "default" : "destructive"}
          className="text-[10px] font-bold"
        >
          WS: {connectionState}
        </Badge>
      </div>

      <div className="flex items-center gap-2">
        <Badge variant="secondary" className="text-xs px-3 py-1 font-bold bg-zinc-800 text-zinc-200 border border-zinc-700">
          Round: {round}
        </Badge>
        <Badge
          variant="default"
          className="text-xs px-3 py-1 font-bold bg-amber-500 text-zinc-950 uppercase tracking-wider"
        >
          {status}
        </Badge>
        {canPauseExit && onPauseExit && (
          <Button
            type="button"
            size="sm"
            variant="outline"
            disabled={isPausing}
            onClick={onPauseExit}
            title="Mette in pausa la partita e torna alla lobby"
            className="gap-1.5 border-sky-500/50 bg-sky-950/40 text-sky-300 hover:text-sky-200 hover:bg-sky-900/50 text-xs font-semibold"
          >
            {isPausing ? <Loader2 className="w-3.5 h-3.5 animate-spin" /> : <Pause className="w-3.5 h-3.5" />}
            {isPausing ? "Pausa..." : "Pausa e torna alla lobby"}
          </Button>
        )}
      </div>
    </div>
  );
}