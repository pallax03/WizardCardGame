"use client";

import { Card as UiCard, CardContent, CardHeader, CardTitle } from "@/ui/components/card";

interface PlayerInfo {
  id: number;
  name: string;
  difficulty?: string | null;
}

interface PlayersRoundStatusProps {
  players: PlayerInfo[];
  currentTurnPlayerId: number | null;
  myPlayerId: number;
  bids: Record<number, number>;
  tricksWon: Record<number, number>;
}

export function PlayersRoundStatus({
  players,
  currentTurnPlayerId,
  myPlayerId,
  bids,
  tricksWon,
}: PlayersRoundStatusProps) {
  return (
    <UiCard className="bg-zinc-900/60 border-zinc-800 md:col-span-2">
      <CardHeader className="pb-2">
        <CardTitle className="text-sm font-semibold uppercase tracking-wider text-zinc-400">
          Players & Round Status
        </CardTitle>
      </CardHeader>
      <CardContent>
        <div className="grid grid-cols-2 sm:grid-cols-3 gap-3">
          {players.map((player) => {
            const isCurrentTurn = currentTurnPlayerId === player.id;
            const isMe = player.id === myPlayerId;
            const playerBid = bids[player.id];
            const playerTricks = tricksWon[player.id] ?? 0;
            const isBot = Boolean(player.difficulty);

            return (
              <div
                key={player.id}
                className={`p-3 rounded-xl border transition-all ${
                  isCurrentTurn
                    ? "bg-indigo-950/40 border-indigo-500 ring-1 ring-indigo-500"
                    : "bg-zinc-950/40 border-zinc-800"
                }`}
              >
                <div className="flex items-center justify-between text-xs mb-1.5">
                  <span className="font-bold text-white truncate max-w-[90px]">
                    {player.name} {isMe && "(You)"}
                  </span>
                  {isBot && (
                    <span className="text-[10px] bg-zinc-800 text-zinc-400 px-1 rounded">
                      Bot
                    </span>
                  )}
                </div>

                <div className="flex items-center justify-between text-xs text-zinc-400 font-mono">
                  <span>Bid: {playerBid !== undefined ? playerBid : "-"}</span>
                  <span>Tricks: {playerTricks}</span>
                </div>

                {isCurrentTurn && (
                  <div className="mt-1.5 text-[10px] font-semibold text-indigo-400 animate-pulse uppercase">
                    Current Turn
                  </div>
                )}
              </div>
            );
          })}
        </div>
      </CardContent>
    </UiCard>
  );
}
