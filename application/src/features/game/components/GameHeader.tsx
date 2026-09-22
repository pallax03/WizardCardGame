"use client";
import Image from "next/image";
import { Badge } from "@/ui/components/badge";
import { Button } from "@/ui/components/button";
import { ArrowLeft, Loader2, Trophy } from "lucide-react";

interface GameHeaderProps {
  lobbyId: string;
  playerId: number | null;
  connectionState: string;
  round: number;
  status: string;
  isPausing?: boolean;
  onBackToLobby?: () => void;
  onToggleScoreboard?: () => void;
}

export function GameHeader({
  lobbyId,
  playerId,
  connectionState,
  round,
  status,
  isPausing,
  onBackToLobby,
  onToggleScoreboard,
}: GameHeaderProps) {
  return (
    <div className="flex items-center justify-between gap-2 p-2 mb-8 sm:p-3 rounded-xl bg-zinc-950/90 border border-zinc-500/30 shadow-xl backdrop-blur-md">
      <div className="flex items-center gap-1.5">
        {onBackToLobby && (
          <Button
            type="button"
            size="icon"
            variant="ghost"
            disabled={isPausing}
            onClick={onBackToLobby}
            title="Mette in pausa la partita e torna alla lobby"
            className="gap-1.5 text-zinc-400 hover:text-zinc-100 text-xs font-semibold"
          >
            {isPausing ? <Loader2 className="w-3.5 h-3.5 animate-spin" /> : <ArrowLeft className="w-3.5 h-3.5" />}
          </Button>
        )}
        <div className="flex flex-col">
          <Image src="/wizard_logo.svg" alt="Wizard Logo" width={50} height={21} className="pointer-events-none" />
          <span className="text-[9px] font-mono text-zinc-400">R:{round}</span>
        </div>
      </div>

      <div className="flex items-center gap-1.5">
        <Button
          type="button"
          size="sm"
          variant="outline"
          onClick={onToggleScoreboard}
          className="h-7 px-2 border-zinc-500/40 bg-zinc-800/50 text-zinc-300 hover:bg-zinc-700/50 text-[10px] font-bold gap-1"
        >
          <Trophy className="size-3" />
          <span className="hidden sm:inline">Punteggi</span>
        </Button>
      </div>
    </div>
  );
}