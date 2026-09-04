"use client";

import { Badge } from "@/ui/components/badge";

interface GameHeaderProps {
  lobbyId: string;
  playerId: number;
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
    <div className="flex flex-wrap items-center justify-between gap-4 p-4 rounded-2xl bg-zinc-900/80 border border-zinc-800 shadow-lg">
      <div className="flex flex-wrap items-center gap-3">
        <h1 className="text-xl font-bold tracking-tight text-white">
          Wizard Game Board
        </h1>
        <Badge variant="outline" className="text-xs bg-zinc-800 border-zinc-700">
          Lobby: <span className="font-mono ml-1 text-indigo-400">{lobbyId}</span>
        </Badge>
        <Badge variant="outline" className="text-xs bg-zinc-800 border-zinc-700">
          Player: <span className="font-mono ml-1 text-emerald-400">#{playerId}</span>
        </Badge>
        <Badge
          variant={connectionState === "open" ? "default" : "destructive"}
          className="text-xs"
        >
          WS: {connectionState}
        </Badge>
      </div>

      <div className="flex items-center gap-2">
        <Badge variant="secondary" className="text-xs px-3 py-1 font-semibold">
          Round: {round}
        </Badge>
        <Badge
          variant="default"
          className="text-xs px-3 py-1 font-semibold bg-indigo-600/80"
        >
          Phase: {status}
        </Badge>
      </div>
    </div>
  );
}
