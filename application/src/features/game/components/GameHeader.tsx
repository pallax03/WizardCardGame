"use client";

import { Badge } from "@/ui/components/badge";
import { Button } from "@/ui/components/button";
import { ArrowLeft, Loader2, Pause, Trophy } from "lucide-react";

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
  onToggleScoreboard?: () => void;
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
  onToggleScoreboard,
}: GameHeaderProps) {
  return (
    <div className="flex items-center justify-between gap-2 p-2 sm:p-3 rounded-xl bg-zinc-950/90 border border-amber-500/30 shadow-xl backdrop-blur-md">
      <div className="flex items-center gap-1.5">
        {onBackHome && (
          <Button
            type="button"
            size="icon"
            variant="ghost"
            disabled={isGoingHome || isPausing}
            onClick={onBackHome}
            title="Home"
            className="size-8 text-zinc-400 hover:text-zinc-100"
          >
            {isGoingHome ? <Loader2 className="size-4 animate-spin" /> : <ArrowLeft className="size-4" />}
          </Button>
        )}
        <div className="flex flex-col">
          <span className="text-xs font-black text-amber-400 leading-none">WIZARD</span>
          <span className="text-[9px] font-mono text-zinc-400">R:{round}</span>
        </div>
      </div>

      <div className="flex items-center gap-1.5">
        <Button
          type="button"
          size="sm"
          variant="outline"
          onClick={onToggleScoreboard}
          className="h-7 px-2 border-amber-500/40 bg-amber-950/30 text-amber-300 hover:bg-amber-900/50 text-[10px] font-bold gap-1"
        >
          <Trophy className="size-3" />
          <span className="hidden sm:inline">Punteggi</span>
        </Button>

        {canPauseExit && onPauseExit && (
          <Button
            type="button"
            size="sm"
            variant="outline"
            disabled={isPausing}
            onClick={onPauseExit}
            className="h-7 px-2 border-sky-500/50 bg-sky-950/40 text-sky-300 text-[10px] font-semibold gap-1"
          >
            {isPausing ? <Loader2 className="size-3 animate-spin" /> : <Pause className="size-3" />}
            <span className="hidden sm:inline">Pausa</span>
          </Button>
        )}
      </div>
    </div>
  );
}