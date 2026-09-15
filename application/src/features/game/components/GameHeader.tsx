"use client";

import { Badge } from "@/ui/components/badge";

interface GameHeaderProps {
  lobbyId: string;
  playerId: number | null;
  connectionState: string;
  round: number;
  status: string;
}

export function GameHeader({
  lobbyId,
  playerId,
  connectionState,
  round,
  status,
}: GameHeaderProps) {
  return (
    <div className="flex flex-wrap items-center justify-between gap-3 p-3 sm:p-4 rounded-2xl bg-zinc-950/90 border border-amber-500/30 shadow-2xl backdrop-blur-md">
      <div className="flex flex-wrap items-center gap-2 sm:gap-3">
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
      </div>
    </div>
  );
}